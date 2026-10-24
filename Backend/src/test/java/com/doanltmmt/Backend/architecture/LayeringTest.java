package com.doanltmmt.Backend.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** P3-05 — controller chỉ nhận request và gọi service: không dùng repository trực tiếp, không quá 120 dòng. */
class LayeringTest {

    private static final int MAX_CONTROLLER_LINES = 120;

    private static List<Class<?>> controllers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<Class<?>> out = new ArrayList<>();
        for (BeanDefinition bd : scanner.findCandidateComponents("com.doanltmmt.Backend")) {
            out.add(Class.forName(bd.getBeanClassName()));
        }
        return out;
    }

    @Test
    void controllersDoNotDependOnRepositories() throws Exception {
        List<Class<?>> all = controllers();
        assertFalse(all.isEmpty());
        List<String> violations = new ArrayList<>();
        for (Class<?> c : all) {
            for (Field f : c.getDeclaredFields()) {
                if (Repository.class.isAssignableFrom(f.getType())) violations.add(c.getSimpleName() + "." + f.getName());
            }
            for (Constructor<?> ctor : c.getDeclaredConstructors()) {
                for (Class<?> p : ctor.getParameterTypes()) {
                    if (Repository.class.isAssignableFrom(p)) violations.add(c.getSimpleName() + "(" + p.getSimpleName() + ")");
                }
            }
        }
        assertTrue(violations.isEmpty(), "Controller dùng repository trực tiếp: " + violations);
    }

    @Test
    void controllersStaySmall() throws Exception {
        List<String> tooLong = new ArrayList<>();
        for (Class<?> c : controllers()) {
            Path source = Path.of("src/main/java", c.getName().replace('.', '/') + ".java");
            long lines = lineCount(source);
            if (lines > MAX_CONTROLLER_LINES) tooLong.add(c.getSimpleName() + " (" + lines + " dòng)");
        }
        assertTrue(tooLong.isEmpty(), "Controller quá " + MAX_CONTROLLER_LINES + " dòng: " + tooLong);
    }

    private static long lineCount(Path p) throws IOException {
        try (var lines = Files.lines(p)) {
            return lines.count();
        }
    }
}
