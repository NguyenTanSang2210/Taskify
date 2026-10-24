package com.doanltmmt.Backend.cluster;

import com.doanltmmt.Backend.BackendApplication;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Notification;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.entity.enums.TopicStatus;
import com.doanltmmt.Backend.entity.enums.WorkspaceStatus;
import com.doanltmmt.Backend.repository.DepartmentRepository;
import com.doanltmmt.Backend.repository.LecturerRepository;
import com.doanltmmt.Backend.repository.RoleRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.repository.WorkspaceRepository;
import com.doanltmmt.Backend.security.JwtTokenProvider;
import com.doanltmmt.Backend.service.EmailService;
import com.doanltmmt.Backend.service.EventPublisher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P4-10 — hai instance backend dùng chung RabbitMQ (STOMP relay) và CSDL: sự kiện phát ở instance A tới được người
 * dùng đang nối WebSocket vào instance B — cả kênh đề tài ({@code /topic/progress.{id}}) lẫn kênh riêng
 * ({@code /topic/notifications.{userId}}, tới mọi tab của người nhận trên mọi instance).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SuppressWarnings("null")
class RealtimeRelayIT {

    private static final String RELAY_USER = "taskify";
    private static final String RELAY_PASSWORD = "mat-khau-relay-thu";

    // chạy bằng user rabbitmq + thư mục dữ liệu tmpfs: không phụ thuộc quyền ghi volume của Docker trên máy chạy test
    private static final GenericContainer<?> RABBIT = new GenericContainer<>(DockerImageName.parse("rabbitmq:4.1.4-management-alpine"))
            .withEnv("RABBITMQ_DEFAULT_USER", RELAY_USER)
            .withEnv("RABBITMQ_DEFAULT_PASS", RELAY_PASSWORD)
            .withCopyToContainer(Transferable.of("[rabbitmq_stomp].\n", 0644), "/etc/rabbitmq/enabled_plugins")
            .withCreateContainerCmdModifier(cmd -> cmd.withUser("rabbitmq"))
            .withTmpFs(Map.of("/var/lib/rabbitmq", "rw,uid=100,gid=101,mode=0700"))
            .withExposedPorts(61613)
            .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1).withStartupTimeout(Duration.ofMinutes(3)));

    static {
        RABBIT.start();
    }

    private static ConfigurableApplicationContext instanceB;

    @DynamicPropertySource
    static void relay(DynamicPropertyRegistry registry) {
        relayProperties().forEach((k, v) -> registry.add(k, () -> v));
    }

    private static Map<String, Object> relayProperties() {
        Map<String, Object> p = new HashMap<>();
        p.put("app.realtime.relay.enabled", "true");
        p.put("app.realtime.relay.host", RABBIT.getHost());
        p.put("app.realtime.relay.port", String.valueOf(RABBIT.getMappedPort(61613)));
        p.put("app.realtime.relay.login", RELAY_USER);
        p.put("app.realtime.relay.passcode", RELAY_PASSWORD);
        return p;
    }

    @LocalServerPort private int portA;
    @Autowired private SimpMessagingTemplate templateA;
    @Autowired private EventPublisher events;
    @Autowired private JwtTokenProvider tokens;
    @Autowired private UserRepository userRepo;
    @Autowired private RoleRepository roleRepo;
    @Autowired private DepartmentRepository deptRepo;
    @Autowired private WorkspaceRepository workspaceRepo;
    @Autowired private LecturerRepository lecturerRepo;
    @Autowired private TopicRepository topicRepo;
    @Autowired private Map<String, HealthIndicator> healthIndicators;
    @Autowired private PlatformTransactionManager txManager;
    @MockitoBean private EmailService emailService;

    private final String run = Long.toString(System.nanoTime(), 36);

    @BeforeEach
    void startSecondInstance() {
        if (instanceB != null) return;
        Map<String, Object> props = relayProperties();
        props.put("server.port", "0");
        props.put("spring.main.banner-mode", "off");
        // tham số dòng lệnh: ưu tiên hơn application.properties (properties() của builder chỉ là giá trị mặc định)
        String[] args = props.entrySet().stream().map(e -> "--" + e.getKey() + "=" + e.getValue()).toArray(String[]::new);
        instanceB = new SpringApplicationBuilder(BackendApplication.class).run(args);
    }

    @AfterAll
    static void stopSecondInstance() {
        if (instanceB != null) instanceB.close();
    }

    @Test
    void eventsPublishedOnA_reachClientConnectedToB() throws Exception {
        awaitBrokerAvailable();
        User admin = newUser("rl_admin", "ADMIN");
        Topic topic = newTopic();

        // hai tab của cùng người dùng: một nối vào A, một nối vào B
        StompSession onB = connect(portOfB(), admin);
        StompSession onA = connect(portA, admin);
        BlockingQueue<Map<?, ?>> progress = subscribe(onB, "/topic/progress." + topic.getId());
        BlockingQueue<Map<?, ?>> personalB = subscribe(onB, "/topic/notifications." + admin.getId());
        BlockingQueue<Map<?, ?>> personalA = subscribe(onA, "/topic/notifications." + admin.getId());
        Thread.sleep(500); // SUBSCRIBE tới RabbitMQ là bất đồng bộ

        templateA.convertAndSend("/topic/progress." + topic.getId(), Map.of("type", "PROGRESS_CREATED", "run", run));
        Notification n = new Notification();
        n.setUser(admin);
        n.setTitle("thong-bao-" + run);
        n.setContent("Kiểm tra relay");
        events.notificationCreated(n); // gửi từ A (ngoài transaction → gửi ngay)

        Map<?, ?> p = progress.poll(10, TimeUnit.SECONDS);
        assertNotNull(p, "kênh đề tài đi qua RabbitMQ sang B");
        assertEquals(run, p.get("run"));
        for (BlockingQueue<Map<?, ?>> tab : List.of(personalB, personalA)) {
            Map<?, ?> payload = tab.poll(10, TimeUnit.SECONDS);
            assertNotNull(payload, "mọi tab của người nhận đều có thông báo, dù nối vào instance nào");
            assertEquals("NOTIFICATION_CREATED", payload.get("type"));
            assertTrue(String.valueOf(payload.get("notification")).contains("thong-bao-" + run), String.valueOf(payload));
            assertNull(tab.poll(1, TimeUnit.SECONDS), "chỉ nhận một lần");
        }
        onA.disconnect();
        onB.disconnect();
    }

    @Test
    void relayHealthIsReported() {
        awaitBrokerAvailable();
        HealthIndicator relay = healthIndicators.get("realtimeBrokerHealthIndicator");
        assertNotNull(relay, "có mục sức khỏe realtimeBroker khi bật relay");
        assertEquals(Status.UP, relay.health().getStatus());
    }

    // ------------------------------------------------------------------ hỗ trợ

    private void awaitBrokerAvailable() {
        HealthIndicator relay = healthIndicators.get("realtimeBrokerHealthIndicator");
        long deadline = System.currentTimeMillis() + 30_000;
        while (relay != null && relay.health().getStatus() != Status.UP && System.currentTimeMillis() < deadline) {
            sleep(200);
        }
    }

    private int portOfB() {
        return Integer.parseInt(instanceB.getEnvironment().getProperty("local.server.port"));
    }

    private StompSession connect(int port, User user) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        StompHeaders connect = new StompHeaders();
        connect.add("Authorization", "Bearer " + tokens.generateAccessToken(user.getUsername(), user.getId(),
                user.getRole().getName(), user.getTokenVersion()));
        return client.connectAsync("ws://localhost:" + port + "/ws/websocket", new WebSocketHttpHeaders(), connect,
                new StompSessionHandlerAdapter() { }).get(10, TimeUnit.SECONDS);
    }

    private static BlockingQueue<Map<?, ?>> subscribe(StompSession session, String destination) {
        BlockingQueue<Map<?, ?>> received = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((Map<?, ?>) payload);
            }
        });
        return received;
    }

    private User newUser(String prefix, String role) {
        User u = new User();
        u.setUsername(prefix + "_" + run);
        u.setPassword("{noop}khong-dung");
        u.setFullName("Người dùng " + prefix);
        u.setEmail(u.getUsername() + "@test.local");
        u.setRole(roleRepo.findByName(role).orElseThrow());
        u.setActive(true);
        return userRepo.save(u);
    }

    /** Giảng viên dùng chung khóa chính với user ({@code @MapsId}) → tạo trong cùng một transaction. */
    private Topic newTopic() {
        return new TransactionTemplate(txManager).execute(status -> createTopic());
    }

    private Topic createTopic() {
        Department d = new Department();
        d.setCode("RL_" + run);
        d.setName("Khoa relay " + run);
        d.setActive(true);
        d = deptRepo.save(d);
        Workspace w = new Workspace();
        w.setDepartment(d);
        w.setName("Đợt relay " + run);
        w.setStatus(WorkspaceStatus.IN_PROGRESS);
        w.setActive(true);
        w = workspaceRepo.save(w);
        User lu = newUser("rl_gv", "LECTURER");
        Lecturer l = new Lecturer();
        l.setUser(lu);
        l.setDepartment(d);
        l = lecturerRepo.save(l);
        Topic t = new Topic();
        t.setTitle("Đề tài relay " + run);
        t.setLecturer(l);
        t.setWorkspace(w);
        t.setCapacity(1);
        t.setStatus(TopicStatus.OPEN);
        return topicRepo.save(t);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
