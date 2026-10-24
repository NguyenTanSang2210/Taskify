-- MySQL dump 10.13  Distrib 8.4.11, for Linux (x86_64)
--
-- Host: localhost    Database: doan_ltmmt
-- ------------------------------------------------------
-- Server version	8.4.11

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `academic_classes`
--

DROP TABLE IF EXISTS `academic_classes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `academic_classes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `code` varchar(50) NOT NULL,
  `name` varchar(255) NOT NULL,
  `department_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKtpbla2txfyucgv7a0tbsyarm0` (`department_id`,`code`),
  CONSTRAINT `FKr6fncacuiempnms4qo75ovf1d` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `academic_classes`
--

LOCK TABLES `academic_classes` WRITE;
/*!40000 ALTER TABLE `academic_classes` DISABLE KEYS */;
INSERT INTO `academic_classes` VALUES (1,_binary '','CNTT-K15','Công nghệ thông tin K15',1);
/*!40000 ALTER TABLE `academic_classes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `announcements`
--

DROP TABLE IF EXISTS `announcements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `title` varchar(255) NOT NULL,
  `author_id` bigint NOT NULL,
  `topic_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK1pgl63iwbqvmngumhvr3xopg3` (`author_id`),
  KEY `FKs4fe698qsc3f6utgex90gwf1q` (`topic_id`),
  CONSTRAINT `FK1pgl63iwbqvmngumhvr3xopg3` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKs4fe698qsc3f6utgex90gwf1q` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `announcements`
--

LOCK TABLES `announcements` WRITE;
/*!40000 ALTER TABLE `announcements` DISABLE KEYS */;
/*!40000 ALTER TABLE `announcements` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `audit_logs`
--

DROP TABLE IF EXISTS `audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(255) DEFAULT NULL,
  `actor_username` varchar(255) DEFAULT NULL,
  `details` text,
  `entity_id` varchar(255) DEFAULT NULL,
  `entity_name` varchar(255) DEFAULT NULL,
  `timestamp` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=388 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_logs`
--

LOCK TABLES `audit_logs` WRITE;
/*!40000 ALTER TABLE `audit_logs` DISABLE KEYS */;
INSERT INTO `audit_logs` VALUES (1,'CREATE_DEPARTMENT','admin','code=T606573','2','Department','2026-09-25 07:50:13.464027'),(2,'CREATE_USER','admin','username=lect606573','6','User','2026-09-25 07:50:13.576071'),(3,'CREATE_WORKSPACE','deptadmin','dept=1','2','Workspace','2026-09-25 07:50:16.091332'),(4,'TRANSITION_WORKSPACE','deptadmin','DRAFT -> OPEN_TOPIC','2','Workspace','2026-09-25 07:50:16.123052'),(5,'ASSIGN_CLASS_TO_WORKSPACE','deptadmin','workspace=2,class=1','1','WorkspaceClass','2026-09-25 07:50:19.405069'),(6,'ASSIGN_LECTURER','deptadmin','workspace=2,lecturer=3,type=MAIN','1','LecturerAssignment','2026-09-25 07:50:19.439533'),(7,'CREATE_TOPIC','deptadmin','Title: Đề tài smoke 606573','1','Topic','2026-09-25 07:50:19.502437'),(8,'TRANSITION_WORKSPACE','deptadmin','OPEN_TOPIC -> OPEN_REGISTRATION','2','Workspace','2026-09-25 07:50:19.534928'),(9,'REGISTER_TOPIC','student','Student SV001 registered topic 1','1','TopicRegistration','2026-09-25 07:50:19.650958'),(10,'REGISTER_TOPIC','student2','Student SV002 registered topic 1','2','TopicRegistration','2026-09-25 07:50:19.759130'),(11,'APPROVE_REGISTRATION','lecturer','Approved registration for topic 1','1','TopicRegistration','2026-09-25 07:50:19.803710'),(12,'TRANSITION_WORKSPACE','deptadmin','OPEN_REGISTRATION -> LOCK_REGISTRATION','2','Workspace','2026-09-25 07:50:22.190720'),(13,'TRANSITION_WORKSPACE','deptadmin','LOCK_REGISTRATION -> IN_PROGRESS','2','Workspace','2026-09-25 07:50:22.209465'),(14,'CREATE_PROGRESS','student','Title: Báo cáo tuần 1','1','ProgressReport','2026-09-25 07:50:22.353708'),(15,'UPDATE_PROGRESS','lecturer','Comment added/Status updated','1','ProgressReport','2026-09-25 07:50:24.711455'),(16,'UPDATE_PROGRESS_STATUS','student','Status changed to: IN_PROGRESS','1','ProgressReport','2026-09-25 07:50:27.016297'),(17,'SEND_MESSAGE','lecturer','To: student','1','Message','2026-09-25 07:50:27.048335'),(18,'GRADE_TOPIC','lecturer','Graded topic 1 with score 8.5','1','TopicRegistration','2026-09-25 07:50:27.108274'),(19,'LOCK_USER','admin','Updated active status to false','4','User','2026-09-25 07:50:29.870206'),(20,'UNLOCK_USER','admin','Updated active status to true','4','User','2026-09-25 07:50:29.921492'),(21,'CREATE_MILESTONE','lect606573','Created milestone: Milestone lạ','1','Milestone','2026-09-25 07:50:30.221017'),(22,'DELETE_MILESTONE','lect606573','Deleted milestone','1','Milestone','2026-09-25 07:50:30.253169'),(23,'CREATE_DEPARTMENT','admin','code=T658255','3','Department','2026-09-25 07:51:03.297181'),(24,'CREATE_USER','admin','username=lect658255','8','User','2026-09-25 07:51:03.373404'),(25,'CREATE_WORKSPACE','deptadmin','dept=1','3','Workspace','2026-09-25 07:51:05.867009'),(26,'TRANSITION_WORKSPACE','deptadmin','DRAFT -> OPEN_TOPIC','3','Workspace','2026-09-25 07:51:05.900318'),(27,'ASSIGN_CLASS_TO_WORKSPACE','deptadmin','workspace=3,class=1','2','WorkspaceClass','2026-09-25 07:51:08.403399'),(28,'ASSIGN_LECTURER','deptadmin','workspace=3,lecturer=3,type=MAIN','2','LecturerAssignment','2026-09-25 07:51:08.438490'),(29,'CREATE_TOPIC','deptadmin','Title: Đề tài smoke 658255','2','Topic','2026-09-25 07:51:08.476766'),(30,'TRANSITION_WORKSPACE','deptadmin','OPEN_TOPIC -> OPEN_REGISTRATION','3','Workspace','2026-09-25 07:51:08.491863'),(31,'REGISTER_TOPIC','student','Student SV001 registered topic 2','3','TopicRegistration','2026-09-25 07:51:08.579992'),(32,'REGISTER_TOPIC','student2','Student SV002 registered topic 2','4','TopicRegistration','2026-09-25 07:51:08.656818'),(33,'APPROVE_REGISTRATION','lecturer','Approved registration for topic 2','3','TopicRegistration','2026-09-25 07:51:08.694336'),(34,'TRANSITION_WORKSPACE','deptadmin','OPEN_REGISTRATION -> LOCK_REGISTRATION','3','Workspace','2026-09-25 07:51:11.067736'),(35,'TRANSITION_WORKSPACE','deptadmin','LOCK_REGISTRATION -> IN_PROGRESS','3','Workspace','2026-09-25 07:51:11.083771'),(36,'CREATE_PROGRESS','student','Title: Báo cáo tuần 1','2','ProgressReport','2026-09-25 07:51:11.174848'),(37,'UPDATE_PROGRESS','lecturer','Comment added/Status updated','2','ProgressReport','2026-09-25 07:51:13.594066'),(38,'UPDATE_PROGRESS_STATUS','student','Status changed to: IN_PROGRESS','2','ProgressReport','2026-09-25 07:51:15.967757'),(39,'SEND_MESSAGE','lecturer','To: student','2','Message','2026-09-25 07:51:15.997620'),(40,'GRADE_TOPIC','lecturer','Graded topic 2 with score 8.5','3','TopicRegistration','2026-09-25 07:51:16.041151'),(41,'LOCK_USER','admin','Updated active status to false','4','User','2026-09-25 07:51:18.509204'),(42,'UNLOCK_USER','admin','Updated active status to true','4','User','2026-09-25 07:51:18.555649'),(43,'CREATE_MILESTONE','lect658255','Created milestone: Milestone lạ','2','Milestone','2026-09-25 07:51:18.799098'),(44,'DELETE_MILESTONE','lect658255','Deleted milestone','2','Milestone','2026-09-25 07:51:18.814754'),(45,'CREATE_DEPARTMENT','admin','code=T896348','4','Department','2026-09-25 07:55:01.176192'),(46,'CREATE_USER','admin','username=lect896348','10','User','2026-09-25 07:55:01.255160'),(47,'CREATE_WORKSPACE','deptadmin','dept=1','4','Workspace','2026-09-25 07:55:03.398143'),(48,'TRANSITION_WORKSPACE','deptadmin','DRAFT -> OPEN_TOPIC','4','Workspace','2026-09-25 07:55:03.411437'),(49,'ASSIGN_CLASS_TO_WORKSPACE','deptadmin','workspace=4,class=1','3','WorkspaceClass','2026-09-25 07:55:05.237410'),(50,'ASSIGN_LECTURER','deptadmin','workspace=4,lecturer=3,type=MAIN','3','LecturerAssignment','2026-09-25 07:55:05.271716'),(51,'CREATE_TOPIC','deptadmin','Title: Đề tài smoke 896348','3','Topic','2026-09-25 07:55:05.312134'),(52,'TRANSITION_WORKSPACE','deptadmin','OPEN_TOPIC -> OPEN_REGISTRATION','4','Workspace','2026-09-25 07:55:05.328301'),(53,'REGISTER_TOPIC','student','Student SV001 registered topic 3','5','TopicRegistration','2026-09-25 07:55:05.411524'),(54,'REGISTER_TOPIC','student2','Student SV002 registered topic 3','6','TopicRegistration','2026-09-25 07:55:05.506229'),(55,'APPROVE_REGISTRATION','lecturer','Approved registration for topic 3','5','TopicRegistration','2026-09-25 07:55:05.550040'),(56,'TRANSITION_WORKSPACE','deptadmin','OPEN_REGISTRATION -> LOCK_REGISTRATION','4','Workspace','2026-09-25 07:55:07.479642'),(57,'TRANSITION_WORKSPACE','deptadmin','LOCK_REGISTRATION -> IN_PROGRESS','4','Workspace','2026-09-25 07:55:07.494001'),(58,'CREATE_PROGRESS','student','Title: Báo cáo tuần 1','3','ProgressReport','2026-09-25 07:55:07.586037'),(59,'UPDATE_PROGRESS','lecturer','Comment added/Status updated','3','ProgressReport','2026-09-25 07:55:09.151489'),(60,'UPDATE_PROGRESS_STATUS','student','Status changed to: IN_PROGRESS','3','ProgressReport','2026-09-25 07:55:10.982399'),(61,'SEND_MESSAGE','lecturer','To: student','3','Message','2026-09-25 07:55:11.012053'),(62,'GRADE_TOPIC','lecturer','Graded topic 3 with score 8.5','5','TopicRegistration','2026-09-25 07:55:11.053477'),(63,'LOCK_USER','admin','Updated active status to false','4','User','2026-09-25 07:55:13.167987'),(64,'UNLOCK_USER','admin','Updated active status to true','4','User','2026-09-25 07:55:13.227049'),(65,'CREATE_MILESTONE','lect896348','Created milestone: Milestone lạ','3','Milestone','2026-09-25 07:55:13.504198'),(66,'DELETE_MILESTONE','lect896348','Deleted milestone','3','Milestone','2026-09-25 07:55:13.535192'),(67,'CREATE_DEPARTMENT','admin','code=T390294','5','Department','2026-09-30 03:36:35.245595'),(68,'CREATE_USER','admin','username=lect390294','12','User','2026-09-30 03:36:35.363001'),(69,'CREATE_WORKSPACE','deptadmin','dept=1','5','Workspace','2026-09-30 03:36:37.373146'),(70,'TRANSITION_WORKSPACE','deptadmin','DRAFT -> OPEN_TOPIC','5','Workspace','2026-09-30 03:36:37.404039'),(71,'ASSIGN_CLASS_TO_WORKSPACE','deptadmin','workspace=5,class=1','4','WorkspaceClass','2026-09-30 03:36:39.634656'),(72,'ASSIGN_LECTURER','deptadmin','workspace=5,lecturer=3,type=MAIN','4','LecturerAssignment','2026-09-30 03:36:39.667169'),(73,'CREATE_TOPIC','deptadmin','Title: Đề tài smoke 390294','4','Topic','2026-09-30 03:36:39.731916'),(74,'TRANSITION_WORKSPACE','deptadmin','OPEN_TOPIC -> OPEN_REGISTRATION','5','Workspace','2026-09-30 03:36:39.762876'),(75,'REGISTER_TOPIC','student','Student SV001 registered topic 4','7','TopicRegistration','2026-09-30 03:36:39.877526'),(76,'REGISTER_TOPIC','student2','Student SV002 registered topic 4','8','TopicRegistration','2026-09-30 03:36:39.972039'),(77,'APPROVE_REGISTRATION','lecturer','Approved registration for topic 4','7','TopicRegistration','2026-09-30 03:36:40.035006'),(78,'TRANSITION_WORKSPACE','deptadmin','OPEN_REGISTRATION -> LOCK_REGISTRATION','5','Workspace','2026-09-30 03:36:41.810581'),(79,'TRANSITION_WORKSPACE','deptadmin','LOCK_REGISTRATION -> IN_PROGRESS','5','Workspace','2026-09-30 03:36:41.830634'),(80,'CREATE_PROGRESS','student','Title: Báo cáo tuần 1','4','ProgressReport','2026-09-30 03:36:41.984020'),(81,'UPDATE_PROGRESS','lecturer','Comment added/Status updated','4','ProgressReport','2026-09-30 03:36:44.065432'),(82,'UPDATE_PROGRESS_STATUS','student','Status changed to: IN_PROGRESS','4','ProgressReport','2026-09-30 03:36:45.942810'),(83,'SEND_MESSAGE','lecturer','To: student','4','Message','2026-09-30 03:36:45.978008'),(84,'GRADE_TOPIC','lecturer','Graded topic 4 with score 8.5','7','TopicRegistration','2026-09-30 03:36:46.032893'),(85,'LOCK_USER','admin','Updated active status to false','4','User','2026-09-30 03:36:48.453852'),(86,'UNLOCK_USER','admin','Updated active status to true','4','User','2026-09-30 03:36:48.498514'),(87,'CREATE_MILESTONE','lect390294','Created milestone: Milestone lạ','4','Milestone','2026-09-30 03:36:48.764559'),(88,'DELETE_MILESTONE','lect390294','Deleted milestone','4','Milestone','2026-09-30 03:36:48.796383'),(89,'CREATE_DEPARTMENT','admin','code=T753781','6','Department','2026-09-30 05:39:18.359052'),(90,'CREATE_USER','admin','username=lect753781','14','User','2026-09-30 05:39:18.451352'),(91,'CREATE_WORKSPACE','deptadmin','dept=1','6','Workspace','2026-09-30 05:39:20.443251'),(92,'TRANSITION_WORKSPACE','deptadmin','DRAFT -> OPEN_TOPIC','6','Workspace','2026-09-30 05:39:20.466620'),(93,'ASSIGN_CLASS_TO_WORKSPACE','deptadmin','workspace=6,class=1','5','WorkspaceClass','2026-09-30 05:39:22.652184'),(94,'ASSIGN_LECTURER','deptadmin','workspace=6,lecturer=3,type=MAIN','5','LecturerAssignment','2026-09-30 05:39:22.686344'),(95,'CREATE_TOPIC','deptadmin','Title: Đề tài smoke 753781','5','Topic','2026-09-30 05:39:22.723813'),(96,'TRANSITION_WORKSPACE','deptadmin','OPEN_TOPIC -> OPEN_REGISTRATION','6','Workspace','2026-09-30 05:39:22.756071'),(97,'REGISTER_TOPIC','student','Student SV001 registered topic 5','9','TopicRegistration','2026-09-30 05:39:22.873973'),(98,'REGISTER_TOPIC','student2','Student SV002 registered topic 5','10','TopicRegistration','2026-09-30 05:39:22.961902'),(99,'APPROVE_REGISTRATION','lecturer','Approved registration for topic 5','9','TopicRegistration','2026-09-30 05:39:23.031843'),(100,'TRANSITION_WORKSPACE','deptadmin','OPEN_REGISTRATION -> LOCK_REGISTRATION','6','Workspace','2026-09-30 05:39:24.832943'),(101,'TRANSITION_WORKSPACE','deptadmin','LOCK_REGISTRATION -> IN_PROGRESS','6','Workspace','2026-09-30 05:39:24.849697'),(102,'CREATE_PROGRESS','student','Title: Báo cáo tuần 1','5','ProgressReport','2026-09-30 05:39:25.002847'),(103,'UPDATE_PROGRESS','lecturer','Comment added/Status updated','5','ProgressReport','2026-09-30 05:39:26.921818'),(104,'UPDATE_PROGRESS_STATUS','student','Status changed to: IN_PROGRESS','5','ProgressReport','2026-09-30 05:39:28.898949'),(105,'SEND_MESSAGE','lecturer','To: student','5','Message','2026-09-30 05:39:28.927665'),(106,'GRADE_TOPIC','lecturer','Graded topic 5 with score 8.5','9','TopicRegistration','2026-09-30 05:39:29.000204'),(107,'LOCK_USER','admin','Updated active status to false','4','User','2026-09-30 05:39:31.538792'),(108,'UNLOCK_USER','admin','Updated active status to true','4','User','2026-09-30 05:39:31.581952'),(109,'CREATE_MILESTONE','lect753781','Created milestone: Milestone lạ','5','Milestone','2026-09-30 05:39:31.803736'),(110,'DELETE_MILESTONE','lect753781','Deleted milestone','5','Milestone','2026-09-30 05:39:31.837787'),(111,'CREATE_DEPARTMENT','admin','code=T922267','7','Department','2026-09-30 05:42:06.166102'),(112,'CREATE_USER','admin','username=lect922267','16','User','2026-09-30 05:42:06.244431'),(113,'CREATE_WORKSPACE','deptadmin','dept=1','7','Workspace','2026-09-30 05:42:08.283305'),(114,'TRANSITION_WORKSPACE','deptadmin','DRAFT -> OPEN_TOPIC','7','Workspace','2026-09-30 05:42:08.295957'),(115,'ASSIGN_CLASS_TO_WORKSPACE','deptadmin','workspace=7,class=1','6','WorkspaceClass','2026-09-30 05:42:10.768574'),(116,'ASSIGN_LECTURER','deptadmin','workspace=7,lecturer=3,type=MAIN','6','LecturerAssignment','2026-09-30 05:42:10.800970'),(117,'CREATE_TOPIC','deptadmin','Title: Đề tài smoke 922267','6','Topic','2026-09-30 05:42:10.832199'),(118,'TRANSITION_WORKSPACE','deptadmin','OPEN_TOPIC -> OPEN_REGISTRATION','7','Workspace','2026-09-30 05:42:10.864297'),(119,'REGISTER_TOPIC','student','Student SV001 registered topic 6','11','TopicRegistration','2026-09-30 05:42:10.948950'),(120,'REGISTER_TOPIC','student2','Student SV002 registered topic 6','12','TopicRegistration','2026-09-30 05:42:11.027055'),(121,'APPROVE_REGISTRATION','lecturer','Approved registration for topic 6','11','TopicRegistration','2026-09-30 05:42:11.064780'),(122,'TRANSITION_WORKSPACE','deptadmin','OPEN_REGISTRATION -> LOCK_REGISTRATION','7','Workspace','2026-09-30 05:42:13.103427'),(123,'TRANSITION_WORKSPACE','deptadmin','LOCK_REGISTRATION -> IN_PROGRESS','7','Workspace','2026-09-30 05:42:13.118307'),(124,'CREATE_PROGRESS','student','Title: Báo cáo tuần 1','6','ProgressReport','2026-09-30 05:42:13.223068'),(125,'UPDATE_PROGRESS','lecturer','Comment added/Status updated','6','ProgressReport','2026-09-30 05:42:14.696543'),(126,'UPDATE_PROGRESS_STATUS','student','Status changed to: IN_PROGRESS','6','ProgressReport','2026-09-30 05:42:16.995772'),(127,'SEND_MESSAGE','lecturer','To: student','6','Message','2026-09-30 05:42:17.027675'),(128,'GRADE_TOPIC','lecturer','Graded topic 6 with score 8.5','11','TopicRegistration','2026-09-30 05:42:17.071733'),(129,'LOCK_USER','admin','Updated active status to false','4','User','2026-09-30 05:42:18.497509'),(130,'UNLOCK_USER','admin','Updated active status to true','4','User','2026-09-30 05:42:18.530649'),(131,'CREATE_MILESTONE','lect922267','Created milestone: Milestone lạ','6','Milestone','2026-09-30 05:42:18.785309'),(132,'DELETE_MILESTONE','lect922267','Deleted milestone','6','Milestone','2026-09-30 05:42:18.806443'),(133,'LOCK_USER','admin','Updated active status to false','4','User','2026-09-30 05:44:10.244224'),(134,'UNLOCK_USER','admin','Updated active status to true','4','User','2026-09-30 05:44:10.362705'),(135,'CHANGE_PASSWORD','student','Password changed','4','User','2026-09-30 05:44:13.405296'),(136,'CHANGE_PASSWORD','student','Password changed','4','User','2026-09-30 05:44:15.660500'),(137,'LOCK_USER','admin','Updated active status to false','4','User','2026-09-30 05:44:39.642444'),(138,'UNLOCK_USER','admin','Updated active status to true','4','User','2026-09-30 05:44:39.761595'),(139,'CHANGE_PASSWORD','student','Password changed','4','User','2026-09-30 05:44:42.788012'),(140,'CHANGE_PASSWORD','student','Password changed','4','User','2026-09-30 05:44:45.036802'),(141,'LOGIN_FAILED','x','ip=172.18.0.1','x','User','2026-09-30 05:53:53.081002'),(142,'LOGIN_FAILED','x','ip=172.18.0.1','x','User','2026-09-30 05:53:59.770161'),(143,'LOGIN_SUCCESS','admin','ip=172.18.0.1','1','User','2026-09-30 05:54:03.723502'),(144,'CREATE_DEPARTMENT','admin','code=T639680','8','Department','2026-09-30 05:54:03.787207'),(145,'CREATE_USER','admin','username=lect639680','18','User','2026-09-30 05:54:03.885664'),(146,'LOGIN_SUCCESS','deptadmin','ip=172.18.0.1','2','User','2026-09-30 05:54:06.063426'),(147,'CREATE_WORKSPACE','deptadmin','dept=1','8','Workspace','2026-09-30 05:54:06.085819'),(148,'TRANSITION_WORKSPACE','deptadmin','DRAFT -> OPEN_TOPIC','8','Workspace','2026-09-30 05:54:06.107883'),(149,'LOGIN_SUCCESS','lecturer','ip=172.18.0.1','3','User','2026-09-30 05:54:07.860681'),(150,'ASSIGN_CLASS_TO_WORKSPACE','deptadmin','workspace=8,class=1','7','WorkspaceClass','2026-09-30 05:54:07.909511'),(151,'ASSIGN_LECTURER','deptadmin','workspace=8,lecturer=3,type=MAIN','7','LecturerAssignment','2026-09-30 05:54:07.948390'),(152,'CREATE_TOPIC','deptadmin','Title: Đề tài smoke 639680','7','Topic','2026-09-30 05:54:07.996285'),(153,'TRANSITION_WORKSPACE','deptadmin','OPEN_TOPIC -> OPEN_REGISTRATION','8','Workspace','2026-09-30 05:54:08.029126'),(154,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-09-30 05:54:08.103253'),(155,'REGISTER_TOPIC','student','Student SV001 registered topic 7','13','TopicRegistration','2026-09-30 05:54:08.142802'),(156,'LOGIN_SUCCESS','student2','ip=172.18.0.1','5','User','2026-09-30 05:54:08.203454'),(157,'REGISTER_TOPIC','student2','Student SV002 registered topic 7','14','TopicRegistration','2026-09-30 05:54:08.239011'),(158,'APPROVE_REGISTRATION','lecturer','Approved registration for topic 7','13','TopicRegistration','2026-09-30 05:54:08.301207'),(159,'TRANSITION_WORKSPACE','deptadmin','OPEN_REGISTRATION -> LOCK_REGISTRATION','8','Workspace','2026-09-30 05:54:09.788162'),(160,'TRANSITION_WORKSPACE','deptadmin','LOCK_REGISTRATION -> IN_PROGRESS','8','Workspace','2026-09-30 05:54:09.805147'),(161,'CREATE_PROGRESS','student','Title: Báo cáo tuần 1','7','ProgressReport','2026-09-30 05:54:09.954818'),(162,'UPDATE_PROGRESS','lecturer','Comment added/Status updated','7','ProgressReport','2026-09-30 05:54:11.420236'),(163,'UPDATE_PROGRESS_STATUS','student','Status changed to: IN_PROGRESS','7','ProgressReport','2026-09-30 05:54:13.236061'),(164,'SEND_MESSAGE','lecturer','To: student','7','Message','2026-09-30 05:54:13.270152'),(165,'GRADE_TOPIC','lecturer','Graded topic 7 with score 8.5','13','TopicRegistration','2026-09-30 05:54:13.328851'),(166,'LOCK_USER','admin','Updated active status to false','4','User','2026-09-30 05:54:15.675436'),(167,'UNLOCK_USER','admin','Updated active status to true','4','User','2026-09-30 05:54:15.705157'),(168,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-09-30 05:54:15.765341'),(169,'LOGIN_SUCCESS','lect639680','ip=172.18.0.1','18','User','2026-09-30 05:54:16.047371'),(170,'CREATE_MILESTONE','lect639680','Created milestone: Milestone lạ','7','Milestone','2026-09-30 05:54:16.079019'),(171,'DELETE_MILESTONE','lect639680','Deleted milestone','7','Milestone','2026-09-30 05:54:16.113262'),(172,'OTP_FAILED','admin','ip=172.18.0.1','1','User','2026-09-30 05:55:06.060209'),(173,'LOGIN_SUCCESS','admin','ip=172.18.0.1','1','User','2026-09-30 05:55:07.302446'),(174,'OTP_FAILED','admin','ip=172.18.0.1','1','User','2026-09-30 05:55:55.191288'),(175,'LOGIN_SUCCESS','admin','ip=172.18.0.1','1','User','2026-09-30 05:55:56.435352'),(176,'OTP_FAILED','admin','ip=172.18.0.1','1','User','2026-09-30 05:56:01.711704'),(177,'OTP_FAILED','admin','ip=172.18.0.1','1','User','2026-09-30 05:56:02.619725'),(178,'OTP_FAILED','admin','ip=172.18.0.1','1','User','2026-09-30 05:56:03.529975'),(179,'OTP_FAILED','admin','ip=172.18.0.1','1','User','2026-09-30 05:56:04.437268'),(180,'OTP_LOCKED','admin','ip=172.18.0.1','1','User','2026-09-30 05:56:05.354214'),(181,'LOGIN_SUCCESS','lecturer','ip=172.18.0.1','3','User','2026-09-30 05:57:13.545044'),(182,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-09-30 05:57:16.237495'),(183,'OTP_FAILED','admin','ip=172.18.0.1','1','User','2026-09-30 05:59:15.938851'),(184,'LOGIN_SUCCESS','admin','ip=172.18.0.1','1','User','2026-09-30 05:59:17.187083'),(185,'OTP_FAILED','admin','ip=172.18.0.1','1','User','2026-09-30 05:59:22.982455'),(186,'OTP_FAILED','admin','ip=172.18.0.1','1','User','2026-09-30 05:59:23.891829'),(187,'OTP_FAILED','admin','ip=172.18.0.1','1','User','2026-09-30 05:59:24.814439'),(188,'OTP_FAILED','admin','ip=172.18.0.1','1','User','2026-09-30 05:59:25.728288'),(189,'OTP_LOCKED','admin','ip=172.18.0.1','1','User','2026-09-30 05:59:26.644200'),(190,'LOGIN_SUCCESS','lecturer','ip=172.18.0.1','3','User','2026-09-30 06:00:35.708103'),(191,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-09-30 06:00:38.397874'),(192,'LOGIN_FAILED','warmup','ip=172.18.0.1','warmup','User','2026-09-30 06:05:38.366853'),(193,'LOGIN_FAILED','ghost-lock-test','ip=172.18.0.1','ghost-lock-test','User','2026-09-30 06:05:38.517768'),(194,'LOGIN_FAILED','ghost-lock-test','ip=172.18.0.1','ghost-lock-test','User','2026-09-30 06:05:38.646177'),(195,'LOGIN_FAILED','ghost-lock-test','ip=172.18.0.1','ghost-lock-test','User','2026-09-30 06:05:38.764383'),(196,'LOGIN_FAILED','ghost-lock-test','ip=172.18.0.1','ghost-lock-test','User','2026-09-30 06:05:38.890482'),(197,'LOGIN_FAILED','ghost-lock-test','ip=172.18.0.1','ghost-lock-test','User','2026-09-30 06:05:38.995852'),(198,'LOGIN_LOCKED','ghost-lock-test','ip=172.18.0.1, khóa 10 phút','ghost-lock-test','User','2026-09-30 06:05:39.000052'),(199,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-09-30 06:05:39.211913'),(200,'LOGIN_FAILED','warmup2','ip=172.18.0.1','warmup2','User','2026-09-30 06:05:54.141642'),(201,'LOGIN_FAILED','x','ip=172.18.0.1','x','User','2026-10-01 02:57:15.462001'),(202,'LOGIN_SUCCESS','admin','ip=172.18.0.1','1','User','2026-10-01 02:57:19.809065'),(203,'CREATE_DEPARTMENT','admin','code=T435288','9','Department','2026-10-01 02:57:19.868328'),(204,'CREATE_USER','admin','username=lect435288','20','User','2026-10-01 02:57:19.981155'),(205,'LOGIN_SUCCESS','deptadmin','ip=172.18.0.1','2','User','2026-10-01 02:57:22.139017'),(206,'CREATE_WORKSPACE','deptadmin','dept=1','9','Workspace','2026-10-01 02:57:22.170866'),(207,'TRANSITION_WORKSPACE','deptadmin','DRAFT -> OPEN_TOPIC','9','Workspace','2026-10-01 02:57:22.192783'),(208,'LOGIN_SUCCESS','lecturer','ip=172.18.0.1','3','User','2026-10-01 02:57:24.337670'),(209,'ASSIGN_CLASS_TO_WORKSPACE','deptadmin','workspace=9,class=1','8','WorkspaceClass','2026-10-01 02:57:24.390229'),(210,'ASSIGN_LECTURER','deptadmin','workspace=9,lecturer=3,type=MAIN','8','LecturerAssignment','2026-10-01 02:57:24.425106'),(211,'CREATE_TOPIC','deptadmin','Title: Đề tài smoke 435288','8','Topic','2026-10-01 02:57:24.496392'),(212,'TRANSITION_WORKSPACE','deptadmin','OPEN_TOPIC -> OPEN_REGISTRATION','9','Workspace','2026-10-01 02:57:24.522918'),(213,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 02:57:24.600915'),(214,'REGISTER_TOPIC','student','Student SV001 registered topic 8','15','TopicRegistration','2026-10-01 02:57:24.636117'),(215,'LOGIN_SUCCESS','student2','ip=172.18.0.1','5','User','2026-10-01 02:57:24.711158'),(216,'REGISTER_TOPIC','student2','Student SV002 registered topic 8','16','TopicRegistration','2026-10-01 02:57:24.749241'),(217,'APPROVE_REGISTRATION','lecturer','Approved registration for topic 8','15','TopicRegistration','2026-10-01 02:57:24.822817'),(218,'TRANSITION_WORKSPACE','deptadmin','OPEN_REGISTRATION -> LOCK_REGISTRATION','9','Workspace','2026-10-01 02:57:26.847719'),(219,'TRANSITION_WORKSPACE','deptadmin','LOCK_REGISTRATION -> IN_PROGRESS','9','Workspace','2026-10-01 02:57:26.863618'),(220,'CREATE_PROGRESS','student','Title: Báo cáo tuần 1','8','ProgressReport','2026-10-01 02:57:27.010123'),(221,'UPDATE_PROGRESS','lecturer','Comment added/Status updated','8','ProgressReport','2026-10-01 02:57:28.902728'),(222,'UPDATE_PROGRESS_STATUS','student','Status changed to: IN_PROGRESS','8','ProgressReport','2026-10-01 02:57:30.818059'),(223,'SEND_MESSAGE','lecturer','To: student','8','Message','2026-10-01 02:57:30.845551'),(224,'GRADE_TOPIC','lecturer','Graded topic 8 with score 8.5','15','TopicRegistration','2026-10-01 02:57:30.921622'),(225,'LOCK_USER','admin','Updated active status to false','4','User','2026-10-01 02:57:33.145299'),(226,'UNLOCK_USER','admin','Updated active status to true','4','User','2026-10-01 02:57:33.190152'),(227,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 02:57:33.255676'),(228,'LOGIN_SUCCESS','lect435288','ip=172.18.0.1','20','User','2026-10-01 02:57:33.485803'),(229,'LOGIN_FAILED','x','ip=172.18.0.1','x','User','2026-10-01 02:58:15.285614'),(230,'LOGIN_SUCCESS','admin','ip=172.18.0.1','1','User','2026-10-01 02:58:19.499798'),(231,'CREATE_DEPARTMENT','admin','code=T495111','10','Department','2026-10-01 02:58:19.548728'),(232,'CREATE_USER','admin','username=lect495111','22','User','2026-10-01 02:58:19.648686'),(233,'LOGIN_SUCCESS','deptadmin','ip=172.18.0.1','2','User','2026-10-01 02:58:21.487211'),(234,'CREATE_WORKSPACE','deptadmin','dept=1','10','Workspace','2026-10-01 02:58:21.517708'),(235,'TRANSITION_WORKSPACE','deptadmin','DRAFT -> OPEN_TOPIC','10','Workspace','2026-10-01 02:58:21.540002'),(236,'LOGIN_SUCCESS','lecturer','ip=172.18.0.1','3','User','2026-10-01 02:58:23.689273'),(237,'ASSIGN_CLASS_TO_WORKSPACE','deptadmin','workspace=10,class=1','9','WorkspaceClass','2026-10-01 02:58:23.740310'),(238,'ASSIGN_LECTURER','deptadmin','workspace=10,lecturer=3,type=MAIN','9','LecturerAssignment','2026-10-01 02:58:23.770590'),(239,'CREATE_TOPIC','deptadmin','Title: Đề tài smoke 495111','9','Topic','2026-10-01 02:58:23.812445'),(240,'TRANSITION_WORKSPACE','deptadmin','OPEN_TOPIC -> OPEN_REGISTRATION','10','Workspace','2026-10-01 02:58:23.843726'),(241,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 02:58:23.921771'),(242,'REGISTER_TOPIC','student','Student SV001 registered topic 9','17','TopicRegistration','2026-10-01 02:58:23.971596'),(243,'LOGIN_SUCCESS','student2','ip=172.18.0.1','5','User','2026-10-01 02:58:24.043827'),(244,'REGISTER_TOPIC','student2','Student SV002 registered topic 9','18','TopicRegistration','2026-10-01 02:58:24.084008'),(245,'APPROVE_REGISTRATION','lecturer','Approved registration for topic 9','17','TopicRegistration','2026-10-01 02:58:24.146566'),(246,'TRANSITION_WORKSPACE','deptadmin','OPEN_REGISTRATION -> LOCK_REGISTRATION','10','Workspace','2026-10-01 02:58:26.164171'),(247,'TRANSITION_WORKSPACE','deptadmin','LOCK_REGISTRATION -> IN_PROGRESS','10','Workspace','2026-10-01 02:58:26.180874'),(248,'CREATE_PROGRESS','student','Title: Báo cáo tuần 1','9','ProgressReport','2026-10-01 02:58:26.334148'),(249,'UPDATE_PROGRESS','lecturer','Comment added/Status updated','9','ProgressReport','2026-10-01 02:58:28.297413'),(250,'UPDATE_PROGRESS_STATUS','student','Status changed to: IN_PROGRESS','9','ProgressReport','2026-10-01 02:58:30.642097'),(251,'SEND_MESSAGE','lecturer','To: student','9','Message','2026-10-01 02:58:30.682150'),(252,'GRADE_TOPIC','lecturer','Graded topic 9 with score 8.5','17','TopicRegistration','2026-10-01 02:58:30.764990'),(253,'LOCK_USER','admin','Updated active status to false','4','User','2026-10-01 02:58:33.463270'),(254,'UNLOCK_USER','admin','Updated active status to true','4','User','2026-10-01 02:58:33.506816'),(255,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 02:58:33.585467'),(256,'LOGIN_SUCCESS','lect495111','ip=172.18.0.1','22','User','2026-10-01 02:58:33.818140'),(257,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 03:01:50.439248'),(258,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 03:02:00.459442'),(259,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 03:02:09.660722'),(260,'CREATE_PROGRESS','student','Title: ws-check 1790823732915','10','ProgressReport','2026-10-01 03:02:13.010884'),(261,'LOGIN_FAILED','x','ip=172.18.0.1','x','User','2026-10-01 03:06:24.150899'),(262,'LOGIN_SUCCESS','admin','ip=172.18.0.1','1','User','2026-10-01 03:06:28.742213'),(263,'CREATE_DEPARTMENT','admin','code=T983992','11','Department','2026-10-01 03:06:28.790654'),(264,'CREATE_USER','admin','username=lect983992','24','User','2026-10-01 03:06:28.913163'),(265,'LOGIN_SUCCESS','deptadmin','ip=172.18.0.1','2','User','2026-10-01 03:06:31.252626'),(266,'CREATE_WORKSPACE','deptadmin','dept=1','11','Workspace','2026-10-01 03:06:31.285941'),(267,'TRANSITION_WORKSPACE','deptadmin','DRAFT -> OPEN_TOPIC','11','Workspace','2026-10-01 03:06:31.314415'),(268,'LOGIN_SUCCESS','lecturer','ip=172.18.0.1','3','User','2026-10-01 03:06:33.627395'),(269,'ASSIGN_CLASS_TO_WORKSPACE','deptadmin','workspace=11,class=1','10','WorkspaceClass','2026-10-01 03:06:33.686484'),(270,'ASSIGN_LECTURER','deptadmin','workspace=11,lecturer=3,type=MAIN','10','LecturerAssignment','2026-10-01 03:06:33.714766'),(271,'CREATE_TOPIC','deptadmin','Title: Đề tài smoke 983992','10','Topic','2026-10-01 03:06:33.757350'),(272,'TRANSITION_WORKSPACE','deptadmin','OPEN_TOPIC -> OPEN_REGISTRATION','11','Workspace','2026-10-01 03:06:33.789461'),(273,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 03:06:33.868419'),(274,'REGISTER_TOPIC','student','Student SV001 registered topic 10','19','TopicRegistration','2026-10-01 03:06:33.914232'),(275,'LOGIN_SUCCESS','student2','ip=172.18.0.1','5','User','2026-10-01 03:06:33.992101'),(276,'REGISTER_TOPIC','student2','Student SV002 registered topic 10','20','TopicRegistration','2026-10-01 03:06:34.032100'),(277,'APPROVE_REGISTRATION','lecturer','Approved registration for topic 10','19','TopicRegistration','2026-10-01 03:06:34.093072'),(278,'TRANSITION_WORKSPACE','deptadmin','OPEN_REGISTRATION -> LOCK_REGISTRATION','11','Workspace','2026-10-01 03:06:36.306690'),(279,'TRANSITION_WORKSPACE','deptadmin','LOCK_REGISTRATION -> IN_PROGRESS','11','Workspace','2026-10-01 03:06:36.323907'),(280,'CREATE_PROGRESS','student','Title: Báo cáo tuần 1','11','ProgressReport','2026-10-01 03:06:36.484834'),(281,'UPDATE_PROGRESS','lecturer','Comment added/Status updated','11','ProgressReport','2026-10-01 03:06:38.688488'),(282,'UPDATE_PROGRESS_STATUS','student','Status changed to: IN_PROGRESS','11','ProgressReport','2026-10-01 03:06:40.809471'),(283,'SEND_MESSAGE','lecturer','To: student','10','Message','2026-10-01 03:06:40.839992'),(284,'GRADE_TOPIC','lecturer','Graded topic 10 with score 8.5','19','TopicRegistration','2026-10-01 03:06:40.910149'),(285,'LOCK_USER','admin','Updated active status to false','4','User','2026-10-01 03:06:43.276041'),(286,'UNLOCK_USER','admin','Updated active status to true','4','User','2026-10-01 03:06:43.322149'),(287,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 03:06:43.401691'),(288,'LOGIN_SUCCESS','lect983992','ip=172.18.0.1','24','User','2026-10-01 03:06:43.647027'),(289,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 03:06:43.945954'),(290,'CREATE_PROGRESS','student','Title: ws-check 1790824007159','12','ProgressReport','2026-10-01 03:06:47.188002'),(291,'LOGIN_FAILED','x','ip=172.18.0.1','x','User','2026-10-01 03:13:50.172111'),(292,'LOGIN_SUCCESS','admin','ip=172.18.0.1','1','User','2026-10-01 03:13:54.990424'),(293,'CREATE_DEPARTMENT','admin','code=T429985','12','Department','2026-10-01 03:13:55.038239'),(294,'CREATE_USER','admin','username=lect429985','26','User','2026-10-01 03:13:55.153445'),(295,'ACTIVATE_ACCOUNT','lect429985','ip=172.18.0.1','26','User','2026-10-01 03:13:57.434679'),(296,'LOGIN_SUCCESS','deptadmin','ip=172.18.0.1','2','User','2026-10-01 03:13:59.692491'),(297,'CREATE_WORKSPACE','deptadmin','dept=1','12','Workspace','2026-10-01 03:13:59.727620'),(298,'TRANSITION_WORKSPACE','deptadmin','DRAFT -> OPEN_TOPIC','12','Workspace','2026-10-01 03:13:59.756760'),(299,'LOGIN_SUCCESS','lecturer','ip=172.18.0.1','3','User','2026-10-01 03:14:01.467362'),(300,'ASSIGN_CLASS_TO_WORKSPACE','deptadmin','workspace=12,class=1','11','WorkspaceClass','2026-10-01 03:14:01.535258'),(301,'ASSIGN_LECTURER','deptadmin','workspace=12,lecturer=3,type=MAIN','11','LecturerAssignment','2026-10-01 03:14:01.558779'),(302,'CREATE_TOPIC','deptadmin','Title: Đề tài smoke 429985','11','Topic','2026-10-01 03:14:01.607741'),(303,'TRANSITION_WORKSPACE','deptadmin','OPEN_TOPIC -> OPEN_REGISTRATION','12','Workspace','2026-10-01 03:14:01.639888'),(304,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 03:14:01.707842'),(305,'REGISTER_TOPIC','student','Student SV001 registered topic 11','21','TopicRegistration','2026-10-01 03:14:01.756730'),(306,'LOGIN_SUCCESS','student2','ip=172.18.0.1','5','User','2026-10-01 03:14:01.841039'),(307,'REGISTER_TOPIC','student2','Student SV002 registered topic 11','22','TopicRegistration','2026-10-01 03:14:01.884399'),(308,'APPROVE_REGISTRATION','lecturer','Approved registration for topic 11','21','TopicRegistration','2026-10-01 03:14:01.940372'),(309,'TRANSITION_WORKSPACE','deptadmin','OPEN_REGISTRATION -> LOCK_REGISTRATION','12','Workspace','2026-10-01 03:14:03.874134'),(310,'TRANSITION_WORKSPACE','deptadmin','LOCK_REGISTRATION -> IN_PROGRESS','12','Workspace','2026-10-01 03:14:03.890459'),(311,'CREATE_PROGRESS','student','Title: Báo cáo tuần 1','13','ProgressReport','2026-10-01 03:14:04.051036'),(312,'UPDATE_PROGRESS','lecturer','Comment added/Status updated','13','ProgressReport','2026-10-01 03:14:06.235413'),(313,'UPDATE_PROGRESS_STATUS','student','Status changed to: IN_PROGRESS','13','ProgressReport','2026-10-01 03:14:08.204288'),(314,'SEND_MESSAGE','lecturer','To: student','11','Message','2026-10-01 03:14:08.240554'),(315,'GRADE_TOPIC','lecturer','Graded topic 11 with score 8.5','21','TopicRegistration','2026-10-01 03:14:08.308749'),(316,'LOCK_USER','admin','Updated active status to false','4','User','2026-10-01 03:14:10.319898'),(317,'UNLOCK_USER','admin','Updated active status to true','4','User','2026-10-01 03:14:10.364921'),(318,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 03:14:10.444255'),(319,'LOGIN_SUCCESS','lect429985','ip=172.18.0.1','26','User','2026-10-01 03:14:12.930591'),(320,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 03:14:13.126892'),(321,'CREATE_PROGRESS','student','Title: ws-check 1790824456348','14','ProgressReport','2026-10-01 03:14:16.377798'),(322,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 03:17:48.914065'),(323,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 03:18:04.587129'),(324,'LOGIN_FAILED','x','ip=172.18.0.1','x','User','2026-10-01 03:23:47.227097'),(325,'LOGIN_SUCCESS','admin','ip=172.18.0.1','1','User','2026-10-01 03:23:51.159862'),(326,'CREATE_DEPARTMENT','admin','code=T027033','13','Department','2026-10-01 03:23:51.218875'),(327,'CREATE_USER','admin','username=lect027033','27','User','2026-10-01 03:23:51.336921'),(328,'ACTIVATE_ACCOUNT','lect027033','ip=172.18.0.1','27','User','2026-10-01 03:23:53.491303'),(329,'LOGIN_SUCCESS','deptadmin','ip=172.18.0.1','2','User','2026-10-01 03:23:55.020960'),(330,'CREATE_WORKSPACE','deptadmin','dept=1','13','Workspace','2026-10-01 03:23:55.045404'),(331,'TRANSITION_WORKSPACE','deptadmin','DRAFT -> OPEN_TOPIC','13','Workspace','2026-10-01 03:23:55.083461'),(332,'LOGIN_SUCCESS','lecturer','ip=172.18.0.1','3','User','2026-10-01 03:23:57.242919'),(333,'ASSIGN_CLASS_TO_WORKSPACE','deptadmin','workspace=13,class=1','12','WorkspaceClass','2026-10-01 03:23:57.293550'),(334,'ASSIGN_LECTURER','deptadmin','workspace=13,lecturer=3,type=MAIN','12','LecturerAssignment','2026-10-01 03:23:57.324254'),(335,'CREATE_TOPIC','deptadmin','Title: Đề tài smoke 027033','12','Topic','2026-10-01 03:23:57.365668'),(336,'TRANSITION_WORKSPACE','deptadmin','OPEN_TOPIC -> OPEN_REGISTRATION','13','Workspace','2026-10-01 03:23:57.396681'),(337,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 03:23:57.474553'),(338,'REGISTER_TOPIC','student','Student SV001 registered topic 12','23','TopicRegistration','2026-10-01 03:23:57.529224'),(339,'LOGIN_SUCCESS','student2','ip=172.18.0.1','5','User','2026-10-01 03:23:57.615356'),(340,'REGISTER_TOPIC','student2','Student SV002 registered topic 12','24','TopicRegistration','2026-10-01 03:23:57.654856'),(341,'APPROVE_REGISTRATION','lecturer','Approved registration for topic 12','23','TopicRegistration','2026-10-01 03:23:57.711201'),(342,'TRANSITION_WORKSPACE','deptadmin','OPEN_REGISTRATION -> LOCK_REGISTRATION','13','Workspace','2026-10-01 03:23:59.741090'),(343,'TRANSITION_WORKSPACE','deptadmin','LOCK_REGISTRATION -> IN_PROGRESS','13','Workspace','2026-10-01 03:23:59.756861'),(344,'CREATE_PROGRESS','student','Title: Báo cáo tuần 1','15','ProgressReport','2026-10-01 03:23:59.923304'),(345,'UPDATE_PROGRESS','lecturer','Comment added/Status updated','15','ProgressReport','2026-10-01 03:24:01.789093'),(346,'UPDATE_PROGRESS_STATUS','student','Status changed to: IN_PROGRESS','15','ProgressReport','2026-10-01 03:24:04.062254'),(347,'SEND_MESSAGE','lecturer','To: student','12','Message','2026-10-01 03:24:04.094912'),(348,'GRADE_TOPIC','lecturer','Graded topic 12 with score 8.5','23','TopicRegistration','2026-10-01 03:24:04.166948'),(349,'LOCK_USER','admin','Updated active status to false','4','User','2026-10-01 03:24:06.332463'),(350,'UNLOCK_USER','admin','Updated active status to true','4','User','2026-10-01 03:24:06.377528'),(351,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 03:24:06.457626'),(352,'LOGIN_SUCCESS','lect027033','ip=172.18.0.1','27','User','2026-10-01 03:24:08.929367'),(353,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 03:24:09.148434'),(354,'CREATE_PROGRESS','student','Title: ws-check 1790825052363','16','ProgressReport','2026-10-01 03:24:12.393734'),(355,'LOGIN_FAILED','x','ip=172.18.0.1','x','User','2026-10-01 03:26:28.243504'),(356,'LOGIN_SUCCESS','admin','ip=172.18.0.1','1','User','2026-10-01 03:26:32.045229'),(357,'CREATE_DEPARTMENT','admin','code=T188143','14','Department','2026-10-01 03:26:32.087429'),(358,'CREATE_USER','admin','username=lect188143','28','User','2026-10-01 03:26:32.170216'),(359,'ACTIVATE_ACCOUNT','lect188143','ip=172.18.0.1','28','User','2026-10-01 03:26:34.320670'),(360,'LOGIN_SUCCESS','deptadmin','ip=172.18.0.1','2','User','2026-10-01 03:26:36.045321'),(361,'CREATE_WORKSPACE','deptadmin','dept=1','14','Workspace','2026-10-01 03:26:36.076052'),(362,'TRANSITION_WORKSPACE','deptadmin','DRAFT -> OPEN_TOPIC','14','Workspace','2026-10-01 03:26:36.103696'),(363,'LOGIN_SUCCESS','lecturer','ip=172.18.0.1','3','User','2026-10-01 03:26:38.271005'),(364,'ASSIGN_CLASS_TO_WORKSPACE','deptadmin','workspace=14,class=1','13','WorkspaceClass','2026-10-01 03:26:38.304842'),(365,'ASSIGN_LECTURER','deptadmin','workspace=14,lecturer=3,type=MAIN','13','LecturerAssignment','2026-10-01 03:26:38.332817'),(366,'CREATE_TOPIC','deptadmin','Title: Đề tài smoke 188143','13','Topic','2026-10-01 03:26:38.377128'),(367,'TRANSITION_WORKSPACE','deptadmin','OPEN_TOPIC -> OPEN_REGISTRATION','14','Workspace','2026-10-01 03:26:38.392432'),(368,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 03:26:38.472453'),(369,'REGISTER_TOPIC','student','Student SV001 registered topic 13','25','TopicRegistration','2026-10-01 03:26:38.507290'),(370,'LOGIN_SUCCESS','student2','ip=172.18.0.1','5','User','2026-10-01 03:26:38.579063'),(371,'REGISTER_TOPIC','student2','Student SV002 registered topic 13','26','TopicRegistration','2026-10-01 03:26:38.614444'),(372,'APPROVE_REGISTRATION','lecturer','Approved registration for topic 13','25','TopicRegistration','2026-10-01 03:26:38.657950'),(373,'TRANSITION_WORKSPACE','deptadmin','OPEN_REGISTRATION -> LOCK_REGISTRATION','14','Workspace','2026-10-01 03:26:40.579523'),(374,'TRANSITION_WORKSPACE','deptadmin','LOCK_REGISTRATION -> IN_PROGRESS','14','Workspace','2026-10-01 03:26:40.595093'),(375,'CREATE_PROGRESS','student','Title: Báo cáo tuần 1','17','ProgressReport','2026-10-01 03:26:40.700784'),(376,'UPDATE_PROGRESS','lecturer','Comment added/Status updated','17','ProgressReport','2026-10-01 03:26:42.415773'),(377,'UPDATE_PROGRESS_STATUS','student','Status changed to: IN_PROGRESS','17','ProgressReport','2026-10-01 03:26:44.440480'),(378,'SEND_MESSAGE','lecturer','To: student','13','Message','2026-10-01 03:26:44.467139'),(379,'GRADE_TOPIC','lecturer','Graded topic 13 with score 8.5','25','TopicRegistration','2026-10-01 03:26:44.505765'),(380,'LOCK_USER','admin','Updated active status to false','4','User','2026-10-01 03:26:46.607631'),(381,'UNLOCK_USER','admin','Updated active status to true','4','User','2026-10-01 03:26:46.639296'),(382,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 03:26:46.719436'),(383,'LOGIN_SUCCESS','lect188143','ip=172.18.0.1','28','User','2026-10-01 03:26:48.357601'),(384,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 03:26:48.562071'),(385,'CREATE_PROGRESS','student','Title: ws-check 1790825211751','18','ProgressReport','2026-10-01 03:26:51.790597'),(386,'LOGIN_SUCCESS','student','ip=172.18.0.1','4','User','2026-10-01 03:26:57.546525'),(387,'LOGIN_SUCCESS','admin','ip=172.18.0.1','1','User','2026-10-23 06:46:05.211703');
/*!40000 ALTER TABLE `audit_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `calendar_events`
--

DROP TABLE IF EXISTS `calendar_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `calendar_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` text,
  `end_time` datetime(6) DEFAULT NULL,
  `start_time` datetime(6) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `type` varchar(20) DEFAULT NULL,
  `topic_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK2uebmbi4goljyd4bea8m5vnab` (`topic_id`),
  CONSTRAINT `FK2uebmbi4goljyd4bea8m5vnab` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `calendar_events`
--

LOCK TABLES `calendar_events` WRITE;
/*!40000 ALTER TABLE `calendar_events` DISABLE KEYS */;
/*!40000 ALTER TABLE `calendar_events` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `departments`
--

DROP TABLE IF EXISTS `departments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `departments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `code` varchar(50) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKl7tivi5261wxdnvo6cct9gg6t` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `departments`
--

LOCK TABLES `departments` WRITE;
/*!40000 ALTER TABLE `departments` DISABLE KEYS */;
INSERT INTO `departments` VALUES (1,_binary '','CNTT','Công nghệ thông tin'),(2,_binary '','T606573','Khoa thử 606573'),(3,_binary '','T658255','Khoa thử 658255'),(4,_binary '','T896348','Khoa thử 896348'),(5,_binary '','T390294','Khoa thử 390294'),(6,_binary '','T753781','Khoa thử 753781'),(7,_binary '','T922267','Khoa thử 922267'),(8,_binary '','T639680','Khoa thử 639680'),(9,_binary '','T435288','Khoa thử 435288'),(10,_binary '','T495111','Khoa thử 495111'),(11,_binary '','T983992','Khoa thử 983992'),(12,_binary '','T429985','Khoa thử 429985'),(13,_binary '','T027033','Khoa thử 027033'),(14,_binary '','T188143','Khoa thử 188143');
/*!40000 ALTER TABLE `departments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `discussion_posts`
--

DROP TABLE IF EXISTS `discussion_posts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `discussion_posts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `author_id` bigint NOT NULL,
  `thread_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKm4m5leisx6to9ts8nuoekw7fn` (`author_id`),
  KEY `FKtcr686u3k5o3sama235eae1ff` (`thread_id`),
  CONSTRAINT `FKm4m5leisx6to9ts8nuoekw7fn` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKtcr686u3k5o3sama235eae1ff` FOREIGN KEY (`thread_id`) REFERENCES `discussion_threads` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `discussion_posts`
--

LOCK TABLES `discussion_posts` WRITE;
/*!40000 ALTER TABLE `discussion_posts` DISABLE KEYS */;
/*!40000 ALTER TABLE `discussion_posts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `discussion_threads`
--

DROP TABLE IF EXISTS `discussion_threads`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `discussion_threads` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `title` varchar(255) NOT NULL,
  `created_by` bigint NOT NULL,
  `topic_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKpvk346hab63rihnrudqu8tpn0` (`created_by`),
  KEY `FK4rf5rba76krjf8s7yx4v16l93` (`topic_id`),
  CONSTRAINT `FK4rf5rba76krjf8s7yx4v16l93` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`),
  CONSTRAINT `FKpvk346hab63rihnrudqu8tpn0` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `discussion_threads`
--

LOCK TABLES `discussion_threads` WRITE;
/*!40000 ALTER TABLE `discussion_threads` DISABLE KEYS */;
/*!40000 ALTER TABLE `discussion_threads` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lecturer_assignments`
--

DROP TABLE IF EXISTS `lecturer_assignments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lecturer_assignments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `quota_max_groups` int NOT NULL,
  `reason` text,
  `type` varchar(20) NOT NULL,
  `lecturer_id` bigint NOT NULL,
  `workspace_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK16a8pxyubeqt9e2vf9yt2t09i` (`workspace_id`,`lecturer_id`,`type`),
  KEY `FKlvfgc661txvu3jncj3baafgh0` (`lecturer_id`),
  CONSTRAINT `FKlvfgc661txvu3jncj3baafgh0` FOREIGN KEY (`lecturer_id`) REFERENCES `lecturers` (`id`),
  CONSTRAINT `FKnq9fgadg0uios2y0b1y1yq60w` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lecturer_assignments`
--

LOCK TABLES `lecturer_assignments` WRITE;
/*!40000 ALTER TABLE `lecturer_assignments` DISABLE KEYS */;
INSERT INTO `lecturer_assignments` VALUES (1,_binary '','2026-09-25 07:50:19.435534',0,NULL,'MAIN',3,2),(2,_binary '','2026-09-25 07:51:08.435087',0,NULL,'MAIN',3,3),(3,_binary '','2026-09-25 07:55:05.268328',0,NULL,'MAIN',3,4),(4,_binary '','2026-09-30 03:36:39.663077',0,NULL,'MAIN',3,5),(5,_binary '','2026-09-30 05:39:22.682452',0,NULL,'MAIN',3,6),(6,_binary '','2026-09-30 05:42:10.797989',0,NULL,'MAIN',3,7),(7,_binary '','2026-09-30 05:54:07.944530',0,NULL,'MAIN',3,8),(8,_binary '','2026-10-01 02:57:24.421825',0,NULL,'MAIN',3,9),(9,_binary '','2026-10-01 02:58:23.769300',0,NULL,'MAIN',3,10),(10,_binary '','2026-10-01 03:06:33.713327',0,NULL,'MAIN',3,11),(11,_binary '','2026-10-01 03:14:01.557351',0,NULL,'MAIN',3,12),(12,_binary '','2026-10-01 03:23:57.322905',0,NULL,'MAIN',3,13),(13,_binary '','2026-10-01 03:26:38.331885',0,NULL,'MAIN',3,14);
/*!40000 ALTER TABLE `lecturer_assignments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lecturers`
--

DROP TABLE IF EXISTS `lecturers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lecturers` (
  `id` bigint NOT NULL,
  `degree` varchar(255) DEFAULT NULL,
  `speciality` varchar(255) DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKpowy6g9rglsdytsdu6c9fymox` (`department_id`),
  CONSTRAINT `FK7d4qvhqq0sr5qryod4h5tcvy5` FOREIGN KEY (`id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKpowy6g9rglsdytsdu6c9fymox` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lecturers`
--

LOCK TABLES `lecturers` WRITE;
/*!40000 ALTER TABLE `lecturers` DISABLE KEYS */;
INSERT INTO `lecturers` VALUES (3,'ThS','Công nghệ phần mềm',1),(6,NULL,NULL,1),(8,NULL,NULL,1),(10,NULL,NULL,1),(12,NULL,NULL,1),(14,NULL,NULL,1),(16,NULL,NULL,1),(18,NULL,NULL,1),(20,NULL,NULL,1),(22,NULL,NULL,1),(24,NULL,NULL,1),(26,NULL,NULL,1),(27,NULL,NULL,1),(28,NULL,NULL,1);
/*!40000 ALTER TABLE `lecturers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `login_throttle`
--

DROP TABLE IF EXISTS `login_throttle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `login_throttle` (
  `throttle_key` varchar(64) NOT NULL,
  `failures` int NOT NULL,
  `locked_until` datetime(6) DEFAULT NULL,
  `window_start` datetime(6) NOT NULL,
  PRIMARY KEY (`throttle_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `login_throttle`
--

LOCK TABLES `login_throttle` WRITE;
/*!40000 ALTER TABLE `login_throttle` DISABLE KEYS */;
INSERT INTO `login_throttle` VALUES ('b2d55c3ae7bfba09eb2b77dd5702cc95e425ea9eb1d856a7951db29c1262b48d',3,NULL,'2026-10-01 03:13:50.152252');
/*!40000 ALTER TABLE `login_throttle` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `messages`
--

DROP TABLE IF EXISTS `messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `recipient_id` bigint NOT NULL,
  `sender_id` bigint NOT NULL,
  `topic_id` bigint DEFAULT NULL,
  `workspace_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKhdkwfnspwb3s60j27vpg0rpg6` (`recipient_id`),
  KEY `FK4ui4nnwntodh6wjvck53dbk9m` (`sender_id`),
  KEY `FKkoe6xlcl0wxc7d5ckn58y1x0c` (`topic_id`),
  KEY `FK31v0eu0kks5tp34l1p11290ox` (`workspace_id`),
  CONSTRAINT `FK31v0eu0kks5tp34l1p11290ox` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`),
  CONSTRAINT `FK4ui4nnwntodh6wjvck53dbk9m` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKhdkwfnspwb3s60j27vpg0rpg6` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKkoe6xlcl0wxc7d5ckn58y1x0c` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `messages`
--

LOCK TABLES `messages` WRITE;
/*!40000 ALTER TABLE `messages` DISABLE KEYS */;
INSERT INTO `messages` VALUES (1,'Hẹn gặp thứ 5 #606573','2026-09-25 07:50:27.043832',4,3,1,2),(2,'Hẹn gặp thứ 5 #658255','2026-09-25 07:51:15.993554',4,3,2,3),(3,'Hẹn gặp thứ 5 #896348','2026-09-25 07:55:11.008838',4,3,3,4),(4,'Hẹn gặp thứ 5 #390294','2026-09-30 03:36:45.971233',4,3,4,5),(5,'Hẹn gặp thứ 5 #753781','2026-09-30 05:39:28.922705',4,3,5,6),(6,'Hẹn gặp thứ 5 #922267','2026-09-30 05:42:17.024570',4,3,6,7),(7,'Hẹn gặp thứ 5 #639680','2026-09-30 05:54:13.265777',4,3,7,8),(8,'Hẹn gặp thứ 5 #435288','2026-10-01 02:57:30.839789',4,3,8,9),(9,'Hẹn gặp thứ 5 #495111','2026-10-01 02:58:30.677813',4,3,9,10),(10,'Hẹn gặp thứ 5 #983992','2026-10-01 03:06:40.835983',4,3,10,11),(11,'Hẹn gặp thứ 5 #429985','2026-10-01 03:14:08.235405',4,3,11,12),(12,'Hẹn gặp thứ 5 #027033','2026-10-01 03:24:04.090495',4,3,12,13),(13,'Hẹn gặp thứ 5 #188143','2026-10-01 03:26:44.464210',4,3,13,14);
/*!40000 ALTER TABLE `messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `milestones`
--

DROP TABLE IF EXISTS `milestones`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `milestones` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `deadline` datetime(6) DEFAULT NULL,
  `description` text,
  `start_date` datetime(6) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `topic_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKnggvk4i34f17w7qy8jyj7r4jt` (`topic_id`),
  CONSTRAINT `FKnggvk4i34f17w7qy8jyj7r4jt` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `milestones`
--

LOCK TABLES `milestones` WRITE;
/*!40000 ALTER TABLE `milestones` DISABLE KEYS */;
/*!40000 ALTER TABLE `milestones` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text,
  `created_at` datetime(6) NOT NULL,
  `due_at` datetime(6) DEFAULT NULL,
  `link_path` varchar(255) DEFAULT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `ref_id` bigint DEFAULT NULL,
  `ref_type` varchar(50) DEFAULT NULL,
  `severity` varchar(30) DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `type` varchar(50) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9y21adhxn0ayjhfocscqox7bh` (`user_id`),
  CONSTRAINT `FK9y21adhxn0ayjhfocscqox7bh` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=76 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
INSERT INTO `notifications` VALUES (1,'De tai #1 - \"Đề tài smoke 606573\" da duoc duyet','2026-09-25 07:50:19.801356',NULL,'/my-registrations',NULL,1,'TOPIC_REGISTRATION','INFO','Dang ky de tai da duoc duyet','INFO',4),(2,'Sinh viên Demo gui \"Báo cáo tuần 1\" cho de tai #1','2026-09-25 07:50:22.360334',NULL,'/lecturer-progress?topicId=1#pr-1',NULL,1,'PROGRESS_REPORT','INFO','Sinh vien gui bao cao #1','INFO',3),(3,'Ban da gui \"Báo cáo tuần 1\" cho de tai #1','2026-09-25 07:50:24.642869',NULL,'/progress?topicId=1#pr-1',NULL,1,'PROGRESS_REPORT','INFO','Da gui bao cao #1','INFO',4),(4,'Nhan xet: Làm tốt, bổ sung sơ đồ use case','2026-09-25 07:50:24.715583',NULL,'/progress?topicId=1#pr-1',NULL,1,'PROGRESS_REPORT','INFO','GV nhan xet bao cao #1','INFO',4),(5,'Diem so: 8.5. Nhan xet: Tốt','2026-09-25 07:50:27.107030',NULL,'/my-registrations',NULL,1,'TOPIC_REGISTRATION','INFO','De tai da co diem tong ket','INFO',4),(6,'De tai #2 - \"Đề tài smoke 658255\" da duoc duyet','2026-09-25 07:51:08.693304',NULL,'/my-registrations',NULL,3,'TOPIC_REGISTRATION','INFO','Dang ky de tai da duoc duyet','INFO',4),(7,'Sinh viên Demo gui \"Báo cáo tuần 1\" cho de tai #2','2026-09-25 07:51:11.178412',NULL,'/lecturer-progress?topicId=2#pr-2',NULL,2,'PROGRESS_REPORT','INFO','Sinh vien gui bao cao #2','INFO',3),(8,'Ban da gui \"Báo cáo tuần 1\" cho de tai #2','2026-09-25 07:51:13.520204',NULL,'/progress?topicId=2#pr-2',NULL,2,'PROGRESS_REPORT','INFO','Da gui bao cao #2','INFO',4),(9,'Nhan xet: Làm tốt, bổ sung sơ đồ use case','2026-09-25 07:51:13.598152',NULL,'/progress?topicId=2#pr-2',NULL,2,'PROGRESS_REPORT','INFO','GV nhan xet bao cao #2','INFO',4),(10,'Diem so: 8.5. Nhan xet: Tốt','2026-09-25 07:51:16.039723',NULL,'/my-registrations',NULL,3,'TOPIC_REGISTRATION','INFO','De tai da co diem tong ket','INFO',4),(11,'De tai #3 - \"Đề tài smoke 896348\" da duoc duyet','2026-09-25 07:55:05.549033',NULL,'/my-registrations',NULL,5,'TOPIC_REGISTRATION','INFO','Dang ky de tai da duoc duyet','INFO',4),(12,'Sinh viên Demo gui \"Báo cáo tuần 1\" cho de tai #3','2026-09-25 07:55:07.590066',NULL,'/lecturer-progress?topicId=3#pr-3',NULL,3,'PROGRESS_REPORT','INFO','Sinh vien gui bao cao #3','INFO',3),(13,'Ban da gui \"Báo cáo tuần 1\" cho de tai #3','2026-09-25 07:55:09.090305',NULL,'/progress?topicId=3#pr-3',NULL,3,'PROGRESS_REPORT','INFO','Da gui bao cao #3','INFO',4),(14,'Nhan xet: Làm tốt, bổ sung sơ đồ use case','2026-09-25 07:55:09.155394',NULL,'/progress?topicId=3#pr-3',NULL,3,'PROGRESS_REPORT','INFO','GV nhan xet bao cao #3','INFO',4),(15,'Diem so: 8.5. Nhan xet: Tốt','2026-09-25 07:55:11.052573',NULL,'/my-registrations',NULL,5,'TOPIC_REGISTRATION','INFO','De tai da co diem tong ket','INFO',4),(16,'De tai #4 - \"Đề tài smoke 390294\" da duoc duyet','2026-09-30 03:36:40.031845',NULL,'/my-registrations',NULL,7,'TOPIC_REGISTRATION','INFO','Dang ky de tai da duoc duyet','INFO',4),(17,'Sinh viên Demo gui \"Báo cáo tuần 1\" cho de tai #4','2026-09-30 03:36:41.992829',NULL,'/lecturer-progress?topicId=4#pr-4',NULL,4,'PROGRESS_REPORT','INFO','Sinh vien gui bao cao #4','INFO',3),(18,'Ban da gui \"Báo cáo tuần 1\" cho de tai #4','2026-09-30 03:36:43.991723',NULL,'/progress?topicId=4#pr-4',NULL,4,'PROGRESS_REPORT','INFO','Da gui bao cao #4','INFO',4),(19,'Nhan xet: Làm tốt, bổ sung sơ đồ use case','2026-09-30 03:36:44.069243',NULL,'/progress?topicId=4#pr-4',NULL,4,'PROGRESS_REPORT','INFO','GV nhan xet bao cao #4','INFO',4),(20,'Diem so: 8.5. Nhan xet: Tốt','2026-09-30 03:36:46.031644',NULL,'/my-registrations',NULL,7,'TOPIC_REGISTRATION','INFO','De tai da co diem tong ket','INFO',4),(21,'De tai #5 - \"Đề tài smoke 753781\" da duoc duyet','2026-09-30 05:39:23.028287',NULL,'/my-registrations',NULL,9,'TOPIC_REGISTRATION','INFO','Dang ky de tai da duoc duyet','INFO',4),(22,'Sinh viên Demo gui \"Báo cáo tuần 1\" cho de tai #5','2026-09-30 05:39:25.008165',NULL,'/lecturer-progress?topicId=5#pr-5',NULL,5,'PROGRESS_REPORT','INFO','Sinh vien gui bao cao #5','INFO',3),(23,'Ban da gui \"Báo cáo tuần 1\" cho de tai #5','2026-09-30 05:39:26.848671',NULL,'/progress?topicId=5#pr-5',NULL,5,'PROGRESS_REPORT','INFO','Da gui bao cao #5','INFO',4),(24,'Nhan xet: Làm tốt, bổ sung sơ đồ use case','2026-09-30 05:39:26.925434',NULL,'/progress?topicId=5#pr-5',NULL,5,'PROGRESS_REPORT','INFO','GV nhan xet bao cao #5','INFO',4),(25,'Diem so: 8.5. Nhan xet: Tốt','2026-09-30 05:39:28.998995',NULL,'/my-registrations',NULL,9,'TOPIC_REGISTRATION','INFO','De tai da co diem tong ket','INFO',4),(26,'De tai #6 - \"Đề tài smoke 922267\" da duoc duyet','2026-09-30 05:42:11.063875',NULL,'/my-registrations',NULL,11,'TOPIC_REGISTRATION','INFO','Dang ky de tai da duoc duyet','INFO',4),(27,'Sinh viên Demo gui \"Báo cáo tuần 1\" cho de tai #6','2026-09-30 05:42:13.226435',NULL,'/lecturer-progress?topicId=6#pr-6',NULL,6,'PROGRESS_REPORT','INFO','Sinh vien gui bao cao #6','INFO',3),(28,'Ban da gui \"Báo cáo tuần 1\" cho de tai #6','2026-09-30 05:42:14.637910',NULL,'/progress?topicId=6#pr-6',NULL,6,'PROGRESS_REPORT','INFO','Da gui bao cao #6','INFO',4),(29,'Nhan xet: Làm tốt, bổ sung sơ đồ use case','2026-09-30 05:42:14.699733',NULL,'/progress?topicId=6#pr-6',NULL,6,'PROGRESS_REPORT','INFO','GV nhan xet bao cao #6','INFO',4),(30,'Diem so: 8.5. Nhan xet: Tốt','2026-09-30 05:42:17.070882',NULL,'/my-registrations',NULL,11,'TOPIC_REGISTRATION','INFO','De tai da co diem tong ket','INFO',4),(31,'De tai #7 - \"Đề tài smoke 639680\" da duoc duyet','2026-09-30 05:54:08.298958',NULL,'/my-registrations',NULL,13,'TOPIC_REGISTRATION','INFO','Dang ky de tai da duoc duyet','INFO',4),(32,'Sinh viên Demo gui \"Báo cáo tuần 1\" cho de tai #7','2026-09-30 05:54:09.959974',NULL,'/lecturer-progress?topicId=7#pr-7',NULL,7,'PROGRESS_REPORT','INFO','Sinh vien gui bao cao #7','INFO',3),(33,'Ban da gui \"Báo cáo tuần 1\" cho de tai #7','2026-09-30 05:54:11.356756',NULL,'/progress?topicId=7#pr-7',NULL,7,'PROGRESS_REPORT','INFO','Da gui bao cao #7','INFO',4),(34,'Nhan xet: Làm tốt, bổ sung sơ đồ use case','2026-09-30 05:54:11.424744',NULL,'/progress?topicId=7#pr-7',NULL,7,'PROGRESS_REPORT','INFO','GV nhan xet bao cao #7','INFO',4),(35,'Diem so: 8.5. Nhan xet: Tốt','2026-09-30 05:54:13.327735',NULL,'/my-registrations',NULL,13,'TOPIC_REGISTRATION','INFO','De tai da co diem tong ket','INFO',4),(36,'De tai #8 - \"Đề tài smoke 435288\" da duoc duyet','2026-10-01 02:57:24.818360',NULL,'/my-registrations',NULL,15,'TOPIC_REGISTRATION','INFO','Dang ky de tai da duoc duyet','INFO',4),(37,'Sinh viên Demo gui \"Báo cáo tuần 1\" cho de tai #8','2026-10-01 02:57:27.015727',NULL,'/lecturer-progress?topicId=8#pr-8',NULL,8,'PROGRESS_REPORT','INFO','Sinh vien gui bao cao #8','INFO',3),(38,'Ban da gui \"Báo cáo tuần 1\" cho de tai #8','2026-10-01 02:57:28.810942',NULL,'/progress?topicId=8#pr-8',NULL,8,'PROGRESS_REPORT','INFO','Da gui bao cao #8','INFO',4),(39,'Nhan xet: Làm tốt, bổ sung sơ đồ use case','2026-10-01 02:57:28.907972',NULL,'/progress?topicId=8#pr-8',NULL,8,'PROGRESS_REPORT','INFO','GV nhan xet bao cao #8','INFO',4),(40,'Diem so: 8.5. Nhan xet: Tốt','2026-10-01 02:57:30.920441',NULL,'/my-registrations',NULL,15,'TOPIC_REGISTRATION','INFO','De tai da co diem tong ket','INFO',4),(41,'De tai #9 - \"Đề tài smoke 495111\" da duoc duyet','2026-10-01 02:58:24.143869',NULL,'/my-registrations',NULL,17,'TOPIC_REGISTRATION','INFO','Dang ky de tai da duoc duyet','INFO',4),(42,'Sinh viên Demo gui \"Báo cáo tuần 1\" cho de tai #9','2026-10-01 02:58:26.339559',NULL,'/lecturer-progress?topicId=9#pr-9',NULL,9,'PROGRESS_REPORT','INFO','Sinh vien gui bao cao #9','INFO',3),(43,'Ban da gui \"Báo cáo tuần 1\" cho de tai #9','2026-10-01 02:58:28.204549',NULL,'/progress?topicId=9#pr-9',NULL,9,'PROGRESS_REPORT','INFO','Da gui bao cao #9','INFO',4),(44,'Nhan xet: Làm tốt, bổ sung sơ đồ use case','2026-10-01 02:58:28.300701',NULL,'/progress?topicId=9#pr-9',NULL,9,'PROGRESS_REPORT','INFO','GV nhan xet bao cao #9','INFO',4),(45,'Diem so: 8.5. Nhan xet: Tốt','2026-10-01 02:58:30.763331',NULL,'/my-registrations',NULL,17,'TOPIC_REGISTRATION','INFO','De tai da co diem tong ket','INFO',4),(46,'Sinh viên Demo gui \"ws-check 1790823732915\" cho de tai #9','2026-10-01 03:02:13.022952',NULL,'/lecturer-progress?topicId=9#pr-10',NULL,10,'PROGRESS_REPORT','INFO','Sinh vien gui bao cao #10','INFO',3),(47,'Ban da gui \"ws-check 1790823732915\" cho de tai #9','2026-10-01 03:02:15.504098',NULL,'/progress?topicId=9#pr-10',NULL,10,'PROGRESS_REPORT','INFO','Da gui bao cao #10','INFO',4),(48,'De tai #10 - \"Đề tài smoke 983992\" da duoc duyet','2026-10-01 03:06:34.089923',NULL,'/my-registrations',NULL,19,'TOPIC_REGISTRATION','INFO','Dang ky de tai da duoc duyet','INFO',4),(49,'Sinh viên Demo gui \"Báo cáo tuần 1\" cho de tai #10','2026-10-01 03:06:36.492669',NULL,'/lecturer-progress?topicId=10#pr-11',NULL,11,'PROGRESS_REPORT','INFO','Sinh vien gui bao cao #11','INFO',3),(50,'Ban da gui \"Báo cáo tuần 1\" cho de tai #10','2026-10-01 03:06:38.601173',NULL,'/progress?topicId=10#pr-11',NULL,11,'PROGRESS_REPORT','INFO','Da gui bao cao #11','INFO',4),(51,'Nhan xet: Làm tốt, bổ sung sơ đồ use case','2026-10-01 03:06:38.692158',NULL,'/progress?topicId=10#pr-11',NULL,11,'PROGRESS_REPORT','INFO','GV nhan xet bao cao #11','INFO',4),(52,'Diem so: 8.5. Nhan xet: Tốt','2026-10-01 03:06:40.908853',NULL,'/my-registrations',NULL,19,'TOPIC_REGISTRATION','INFO','De tai da co diem tong ket','INFO',4),(53,'Sinh viên Demo gui \"ws-check 1790824007159\" cho de tai #10','2026-10-01 03:06:47.191017',NULL,'/lecturer-progress?topicId=10#pr-12',NULL,12,'PROGRESS_REPORT','INFO','Sinh vien gui bao cao #12','INFO',3),(54,'Ban da gui \"ws-check 1790824007159\" cho de tai #10','2026-10-01 03:06:49.066810',NULL,'/progress?topicId=10#pr-12',NULL,12,'PROGRESS_REPORT','INFO','Da gui bao cao #12','INFO',4),(55,'De tai #11 - \"Đề tài smoke 429985\" da duoc duyet','2026-10-01 03:14:01.936945',NULL,'/my-registrations',NULL,21,'TOPIC_REGISTRATION','INFO','Dang ky de tai da duoc duyet','INFO',4),(56,'Sinh viên Demo gui \"Báo cáo tuần 1\" cho de tai #11','2026-10-01 03:14:04.058044',NULL,'/lecturer-progress?topicId=11#pr-13',NULL,13,'PROGRESS_REPORT','INFO','Sinh vien gui bao cao #13','INFO',3),(57,'Ban da gui \"Báo cáo tuần 1\" cho de tai #11','2026-10-01 03:14:06.152199',NULL,'/progress?topicId=11#pr-13',NULL,13,'PROGRESS_REPORT','INFO','Da gui bao cao #13','INFO',4),(58,'Nhan xet: Làm tốt, bổ sung sơ đồ use case','2026-10-01 03:14:06.239676',NULL,'/progress?topicId=11#pr-13',NULL,13,'PROGRESS_REPORT','INFO','GV nhan xet bao cao #13','INFO',4),(59,'Diem so: 8.5. Nhan xet: Tốt','2026-10-01 03:14:08.307417',NULL,'/my-registrations',NULL,21,'TOPIC_REGISTRATION','INFO','De tai da co diem tong ket','INFO',4),(60,'Sinh viên Demo gui \"ws-check 1790824456348\" cho de tai #11','2026-10-01 03:14:16.380967',NULL,'/lecturer-progress?topicId=11#pr-14',NULL,14,'PROGRESS_REPORT','INFO','Sinh vien gui bao cao #14','INFO',3),(61,'Ban da gui \"ws-check 1790824456348\" cho de tai #11','2026-10-01 03:14:18.233128',NULL,'/progress?topicId=11#pr-14',NULL,14,'PROGRESS_REPORT','INFO','Da gui bao cao #14','INFO',4),(62,'De tai #12 - \"Đề tài smoke 027033\" da duoc duyet','2026-10-01 03:23:57.707603',NULL,'/my-registrations',NULL,23,'TOPIC_REGISTRATION','INFO','Dang ky de tai da duoc duyet','INFO',4),(63,'Sinh viên Demo gui \"Báo cáo tuần 1\" cho de tai #12','2026-10-01 03:23:59.929770',NULL,'/lecturer-progress?topicId=12#pr-15',NULL,15,'PROGRESS_REPORT','INFO','Sinh vien gui bao cao #15','INFO',3),(64,'Ban da gui \"Báo cáo tuần 1\" cho de tai #12','2026-10-01 03:24:01.688541',NULL,'/progress?topicId=12#pr-15',NULL,15,'PROGRESS_REPORT','INFO','Da gui bao cao #15','INFO',4),(65,'Nhan xet: Làm tốt, bổ sung sơ đồ use case','2026-10-01 03:24:01.792366',NULL,'/progress?topicId=12#pr-15',NULL,15,'PROGRESS_REPORT','INFO','GV nhan xet bao cao #15','INFO',4),(66,'Diem so: 8.5. Nhan xet: Tốt','2026-10-01 03:24:04.165721',NULL,'/my-registrations',NULL,23,'TOPIC_REGISTRATION','INFO','De tai da co diem tong ket','INFO',4),(67,'Sinh viên Demo gui \"ws-check 1790825052363\" cho de tai #12','2026-10-01 03:24:12.396728',NULL,'/lecturer-progress?topicId=12#pr-16',NULL,16,'PROGRESS_REPORT','INFO','Sinh vien gui bao cao #16','INFO',3),(68,'Ban da gui \"ws-check 1790825052363\" cho de tai #12','2026-10-01 03:24:14.261131',NULL,'/progress?topicId=12#pr-16',NULL,16,'PROGRESS_REPORT','INFO','Da gui bao cao #16','INFO',4),(69,'De tai #13 - \"Đề tài smoke 188143\" da duoc duyet','2026-10-01 03:26:38.656993',NULL,'/my-registrations',NULL,25,'TOPIC_REGISTRATION','INFO','Dang ky de tai da duoc duyet','INFO',4),(70,'Sinh viên Demo gui \"Báo cáo tuần 1\" cho de tai #13','2026-10-01 03:26:40.704220',NULL,'/lecturer-progress?topicId=13#pr-17',NULL,17,'PROGRESS_REPORT','INFO','Sinh vien gui bao cao #17','INFO',3),(71,'Ban da gui \"Báo cáo tuần 1\" cho de tai #13','2026-10-01 03:26:42.341186',NULL,'/progress?topicId=13#pr-17',NULL,17,'PROGRESS_REPORT','INFO','Da gui bao cao #17','INFO',4),(72,'Nhan xet: Làm tốt, bổ sung sơ đồ use case','2026-10-01 03:26:42.418902',NULL,'/progress?topicId=13#pr-17',NULL,17,'PROGRESS_REPORT','INFO','GV nhan xet bao cao #17','INFO',4),(73,'Diem so: 8.5. Nhan xet: Tốt','2026-10-01 03:26:44.504782',NULL,'/my-registrations',NULL,25,'TOPIC_REGISTRATION','INFO','De tai da co diem tong ket','INFO',4),(74,'Sinh viên Demo gui \"ws-check 1790825211751\" cho de tai #13','2026-10-01 03:26:51.794859',NULL,'/lecturer-progress?topicId=13#pr-18',NULL,18,'PROGRESS_REPORT','INFO','Sinh vien gui bao cao #18','INFO',3),(75,'Ban da gui \"ws-check 1790825211751\" cho de tai #13','2026-10-01 03:26:53.734933',NULL,'/progress?topicId=13#pr-18',NULL,18,'PROGRESS_REPORT','INFO','Da gui bao cao #18','INFO',4);
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `otp_tokens`
--

DROP TABLE IF EXISTS `otp_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `otp_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(10) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `used` bit(1) NOT NULL,
  `user_id` bigint NOT NULL,
  `attempts` int NOT NULL DEFAULT '0',
  `created_at` datetime(6) DEFAULT NULL,
  `purpose` enum('ACTIVATION','LOGIN','RESET_PASSWORD') NOT NULL DEFAULT 'LOGIN',
  PRIMARY KEY (`id`),
  KEY `FKjyk2kyj8ul2bjqjpentlrpyi1` (`user_id`),
  CONSTRAINT `FKjyk2kyj8ul2bjqjpentlrpyi1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=88 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `otp_tokens`
--

LOCK TABLES `otp_tokens` WRITE;
/*!40000 ALTER TABLE `otp_tokens` DISABLE KEYS */;
INSERT INTO `otp_tokens` VALUES (1,'139397','2026-09-25 07:55:06.769092',_binary '',1,0,NULL,'LOGIN'),(2,'350591','2026-09-25 07:55:09.749912',_binary '',1,0,NULL,'LOGIN'),(3,'743007','2026-09-25 07:55:13.638496',_binary '',2,0,NULL,'LOGIN'),(4,'158302','2026-09-25 07:55:16.194424',_binary '',3,0,NULL,'LOGIN'),(5,'416023','2026-09-25 07:55:30.046430',_binary '',6,0,NULL,'LOGIN'),(6,'175094','2026-09-25 07:55:58.410503',_binary '',1,0,NULL,'LOGIN'),(7,'303132','2026-09-25 07:56:00.812085',_binary '',1,0,NULL,'LOGIN'),(8,'219376','2026-09-25 07:56:03.437135',_binary '',2,0,NULL,'LOGIN'),(9,'259697','2026-09-25 07:56:05.961281',_binary '',3,0,NULL,'LOGIN'),(10,'646722','2026-09-25 07:56:18.648462',_binary '',8,0,NULL,'LOGIN'),(11,'172100','2026-09-25 07:57:37.464728',_binary '',3,0,NULL,'LOGIN'),(12,'375744','2026-09-25 07:59:56.506622',_binary '',1,0,NULL,'LOGIN'),(13,'481830','2026-09-25 07:59:58.852352',_binary '',1,0,NULL,'LOGIN'),(14,'560009','2026-09-25 08:00:01.328716',_binary '',2,0,NULL,'LOGIN'),(15,'443669','2026-09-25 08:00:03.470028',_binary '',3,0,NULL,'LOGIN'),(16,'502162','2026-09-25 08:00:13.332706',_binary '',10,0,NULL,'LOGIN'),(17,'657828','2026-09-30 03:41:30.483303',_binary '',1,0,NULL,'LOGIN'),(18,'859682','2026-09-30 03:41:32.993196',_binary '',1,0,NULL,'LOGIN'),(19,'235922','2026-09-30 03:41:35.430772',_binary '',2,0,NULL,'LOGIN'),(20,'508783','2026-09-30 03:41:37.467903',_binary '',3,0,NULL,'LOGIN'),(21,'857227','2026-09-30 03:41:48.618067',_binary '',12,0,NULL,'LOGIN'),(22,'727773','2026-09-30 05:44:04.170886',_binary '',3,0,NULL,'LOGIN'),(23,'964879','2026-09-30 05:44:13.960205',_binary '',1,0,NULL,'LOGIN'),(24,'789737','2026-09-30 05:44:16.080593',_binary '',1,0,NULL,'LOGIN'),(25,'392277','2026-09-30 05:44:18.523452',_binary '',2,0,NULL,'LOGIN'),(26,'015317','2026-09-30 05:44:20.527261',_binary '',3,0,NULL,'LOGIN'),(27,'626461','2026-09-30 05:44:31.674919',_binary '',14,0,NULL,'LOGIN'),(28,'027496','2026-09-30 05:47:02.420681',_binary '',1,0,NULL,'LOGIN'),(29,'458425','2026-09-30 05:47:04.392573',_binary '',1,0,NULL,'LOGIN'),(30,'657236','2026-09-30 05:47:06.305419',_binary '',2,0,NULL,'LOGIN'),(31,'963996','2026-09-30 05:47:08.357355',_binary '',3,0,NULL,'LOGIN'),(32,'723447','2026-09-30 05:47:18.677901',_binary '',16,0,NULL,'LOGIN'),(33,'766989','2026-09-30 05:49:02.918817',_binary '',1,0,NULL,'LOGIN'),(34,'588294','2026-09-30 05:49:33.940741',_binary '',1,0,NULL,'LOGIN'),(35,'489807','2026-09-30 05:58:59.847431',_binary '',1,0,'2026-09-30 05:53:59.847927','LOGIN'),(36,'081181','2026-09-30 05:59:02.136567',_binary '',1,0,'2026-09-30 05:54:02.136709','LOGIN'),(37,'987592','2026-09-30 05:59:03.956129',_binary '',2,0,'2026-09-30 05:54:03.956268','LOGIN'),(38,'812790','2026-09-30 05:59:06.175432',_binary '',3,0,'2026-09-30 05:54:06.175494','LOGIN'),(39,'117238','2026-09-30 05:59:15.939315',_binary '',18,0,'2026-09-30 05:54:15.939463','LOGIN'),(40,'668019','2026-09-30 06:00:02.990585',_binary '',1,1,'2026-09-30 05:55:02.990896','LOGIN'),(41,'668274','2026-09-30 06:00:52.614468',_binary '',1,1,'2026-09-30 05:55:52.614678','LOGIN'),(42,'891390','2026-09-30 06:00:59.700313',_binary '',1,5,'2026-09-30 05:55:59.700508','LOGIN'),(43,'638551','2026-09-30 06:01:07.831107',_binary '',3,0,'2026-09-30 05:56:07.831303','LOGIN'),(44,'267791','2026-09-30 06:02:11.851252',_binary '',3,0,'2026-09-30 05:57:11.851338','LOGIN'),(45,'826497','2026-09-30 06:02:48.254066',_binary '',3,0,'2026-09-30 05:57:48.254213','LOGIN'),(46,'988922','2026-09-30 06:03:52.615230',_binary '',3,0,'2026-09-30 05:58:52.615306','LOGIN'),(47,'495246','2026-09-30 06:04:13.898715',_binary '',1,1,'2026-09-30 05:59:13.898838','LOGIN'),(48,'735906','2026-09-30 06:04:20.468962',_binary '',1,5,'2026-09-30 05:59:20.469094','LOGIN'),(49,'986263','2026-09-30 06:04:29.130723',_binary '',3,0,'2026-09-30 05:59:29.130809','LOGIN'),(50,'477537','2026-09-30 06:05:33.147129',_binary '',3,0,'2026-09-30 06:00:33.147205','LOGIN'),(51,'983427','2026-10-01 03:02:15.554326',_binary '',1,0,'2026-10-01 02:57:15.554890','LOGIN'),(52,'681639','2026-10-01 03:02:17.694528',_binary '',1,0,'2026-10-01 02:57:17.694654','LOGIN'),(53,'149431','2026-10-01 03:02:20.057374',_binary '',2,0,'2026-10-01 02:57:20.057491','LOGIN'),(54,'676251','2026-10-01 03:02:22.262709',_binary '',3,0,'2026-10-01 02:57:22.262886','LOGIN'),(55,'218631','2026-10-01 03:02:33.363867',_binary '',20,0,'2026-10-01 02:57:33.363999','LOGIN'),(56,'187931','2026-10-01 03:03:15.373415',_binary '',1,0,'2026-10-01 02:58:15.373886','LOGIN'),(57,'944785','2026-10-01 03:03:17.482203',_binary '',1,0,'2026-10-01 02:58:17.482324','LOGIN'),(58,'525424','2026-10-01 03:03:19.719109',_binary '',2,0,'2026-10-01 02:58:19.719258','LOGIN'),(59,'652031','2026-10-01 03:03:21.624852',_binary '',3,0,'2026-10-01 02:58:21.625140','LOGIN'),(60,'751068','2026-10-01 03:03:33.705266',_binary '',22,0,'2026-10-01 02:58:33.705397','LOGIN'),(61,'745847','2026-10-01 03:11:24.235316',_binary '',1,0,'2026-10-01 03:06:24.235858','LOGIN'),(62,'303274','2026-10-01 03:11:26.493297',_binary '',1,0,'2026-10-01 03:06:26.493423','LOGIN'),(63,'830082','2026-10-01 03:11:29.016872',_binary '',2,0,'2026-10-01 03:06:29.017035','LOGIN'),(64,'493957','2026-10-01 03:11:31.397013',_binary '',3,0,'2026-10-01 03:06:31.397160','LOGIN'),(65,'067212','2026-10-01 03:11:43.527186',_binary '',24,0,'2026-10-01 03:06:43.527386','LOGIN'),(66,'092250','2026-10-01 03:18:50.267402',_binary '',1,0,'2026-10-01 03:13:50.268021','LOGIN'),(67,'967026','2026-10-01 03:18:52.765436',_binary '',1,0,'2026-10-01 03:13:52.765528','LOGIN'),(68,'311174','2026-10-04 03:13:55.157771',_binary '',26,0,'2026-10-01 03:13:55.157905','ACTIVATION'),(69,'502785','2026-10-01 03:18:57.511843',_binary '',2,0,'2026-10-01 03:13:57.511976','LOGIN'),(70,'827542','2026-10-01 03:18:59.830998',_binary '',3,0,'2026-10-01 03:13:59.831112','LOGIN'),(71,'506131','2026-10-01 03:19:10.551417',_binary '',26,0,'2026-10-01 03:14:10.551535','LOGIN'),(72,'431918','2026-10-01 03:22:52.883351',_binary '',1,0,'2026-10-01 03:17:52.884024','LOGIN'),(73,'208733','2026-10-01 03:23:09.757656',_binary '',1,0,'2026-10-01 03:18:09.757746','LOGIN'),(74,'155810','2026-10-01 03:28:47.324992',_binary '',1,0,'2026-10-01 03:23:47.325497','LOGIN'),(75,'414649','2026-10-01 03:28:49.282166',_binary '',1,0,'2026-10-01 03:23:49.282246','LOGIN'),(76,'351028','2026-10-04 03:23:51.340841',_binary '',27,0,'2026-10-01 03:23:51.340964','ACTIVATION'),(77,'315731','2026-10-01 03:28:53.571901',_binary '',2,0,'2026-10-01 03:23:53.572047','LOGIN'),(78,'653444','2026-10-01 03:28:55.162690',_binary '',3,0,'2026-10-01 03:23:55.162865','LOGIN'),(79,'274421','2026-10-01 03:29:06.583702',_binary '',27,0,'2026-10-01 03:24:06.583845','LOGIN'),(80,'144096','2026-10-01 03:31:28.318861',_binary '',1,0,'2026-10-01 03:26:28.319008','LOGIN'),(81,'605435','2026-10-01 03:31:29.871337',_binary '',1,0,'2026-10-01 03:26:29.871430','LOGIN'),(82,'186766','2026-10-04 03:26:32.174116',_binary '',28,0,'2026-10-01 03:26:32.174307','ACTIVATION'),(83,'021604','2026-10-01 03:31:34.396795',_binary '',2,0,'2026-10-01 03:26:34.396950','LOGIN'),(84,'852383','2026-10-01 03:31:36.167944',_binary '',3,0,'2026-10-01 03:26:36.168105','LOGIN'),(85,'792906','2026-10-01 03:31:46.829185',_binary '',28,0,'2026-10-01 03:26:46.829342','LOGIN'),(86,'009611','2026-10-01 03:32:02.744064',_binary '',1,0,'2026-10-01 03:27:02.744281','LOGIN'),(87,'977167','2026-10-23 06:50:38.166749',_binary '',1,0,'2026-10-23 06:45:38.167812','LOGIN');
/*!40000 ALTER TABLE `otp_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `privileges`
--

DROP TABLE IF EXISTS `privileges`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `privileges` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKm2tnonbcaquofx1ccy060ejyc` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `privileges`
--

LOCK TABLES `privileges` WRITE;
/*!40000 ALTER TABLE `privileges` DISABLE KEYS */;
INSERT INTO `privileges` VALUES (1,'Quản lý người dùng (Xem, Thêm, Sửa, Xóa)','USER_MANAGE'),(2,'Quản lý vai trò và quyền hạn','ROLE_MANAGE'),(3,'Quản lý khoa và lớp học','DEPT_MANAGE'),(4,'Quản lý đề tài (Phê duyệt, Đóng/Mở)','TOPIC_MANAGE'),(5,'Duyệt đăng ký đề tài','REGISTRATION_MANAGE'),(6,'Xem báo cáo và thống kê hệ thống','SYSTEM_REPORT');
/*!40000 ALTER TABLE `privileges` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `progress_report`
--

DROP TABLE IF EXISTS `progress_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `progress_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deadline` datetime(6) DEFAULT NULL,
  `file_url` varchar(500) DEFAULT NULL,
  `lecturer_comment` text,
  `status` varchar(20) NOT NULL,
  `title` varchar(200) NOT NULL,
  `milestone_id` bigint DEFAULT NULL,
  `student_id` bigint NOT NULL,
  `topic_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKq7cpn58kdpb93a1hxu6hjvo25` (`milestone_id`),
  KEY `FKo1e7ufhbx0pt1wpv1hapvuhto` (`student_id`),
  KEY `FKb8w6aglicr9nk6gly2qb53h6f` (`topic_id`),
  CONSTRAINT `FKb8w6aglicr9nk6gly2qb53h6f` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`),
  CONSTRAINT `FKo1e7ufhbx0pt1wpv1hapvuhto` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `FKq7cpn58kdpb93a1hxu6hjvo25` FOREIGN KEY (`milestone_id`) REFERENCES `milestones` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `progress_report`
--

LOCK TABLES `progress_report` WRITE;
/*!40000 ALTER TABLE `progress_report` DISABLE KEYS */;
INSERT INTO `progress_report` VALUES (1,'Đã hoàn thành phân tích yêu cầu','2026-09-25 07:50:22.347061',NULL,'/api/secure/files/2d769377-ca55-49fe-99c9-1f684148815c_smoke-606573.txt','Làm tốt, bổ sung sơ đồ use case','IN_PROGRESS','Báo cáo tuần 1',NULL,4,1),(2,'Đã hoàn thành phân tích yêu cầu','2026-09-25 07:51:11.171233',NULL,'/api/secure/files/e0f3373e-94ab-4b4d-b772-c23e0b48c7b0_smoke-658255.txt','Làm tốt, bổ sung sơ đồ use case','IN_PROGRESS','Báo cáo tuần 1',NULL,4,2),(3,'Đã hoàn thành phân tích yêu cầu','2026-09-25 07:55:07.582883',NULL,'/api/secure/files/abd22052-d5a4-4007-93a0-e127afd469a2_smoke-896348.txt','Làm tốt, bổ sung sơ đồ use case','IN_PROGRESS','Báo cáo tuần 1',NULL,4,3),(4,'Đã hoàn thành phân tích yêu cầu','2026-09-30 03:36:41.976065',NULL,'/api/secure/files/ac0cc914-edcb-453d-b318-1d7a993ba87e_smoke-390294.txt','Làm tốt, bổ sung sơ đồ use case','IN_PROGRESS','Báo cáo tuần 1',NULL,4,4),(5,'Đã hoàn thành phân tích yêu cầu','2026-09-30 05:39:24.996607',NULL,'/api/secure/files/2e4e86b8-0293-478e-a445-a69a22e37a69_smoke-753781.txt','Làm tốt, bổ sung sơ đồ use case','IN_PROGRESS','Báo cáo tuần 1',NULL,4,5),(6,'Đã hoàn thành phân tích yêu cầu','2026-09-30 05:42:13.218598',NULL,'/api/secure/files/7d92d675-96ed-47e5-86c3-fff7d6300130_smoke-922267.txt','Làm tốt, bổ sung sơ đồ use case','IN_PROGRESS','Báo cáo tuần 1',NULL,4,6),(7,'Đã hoàn thành phân tích yêu cầu','2026-09-30 05:54:09.949736',NULL,'/api/secure/files/49eb57e1-ce33-4833-a4c3-0715aae5b0f0_smoke-639680.txt','Làm tốt, bổ sung sơ đồ use case','IN_PROGRESS','Báo cáo tuần 1',NULL,4,7),(8,'Đã hoàn thành phân tích yêu cầu','2026-10-01 02:57:27.005389',NULL,'/api/secure/files/39a1473a-a8ce-4b9e-8af4-2df2e613bde1_smoke-435288.txt','Làm tốt, bổ sung sơ đồ use case','IN_PROGRESS','Báo cáo tuần 1',NULL,4,8),(9,'Đã hoàn thành phân tích yêu cầu','2026-10-01 02:58:26.329625',NULL,'/api/secure/files/587d70ac-7531-41eb-81ef-eff0cf218f89_smoke-495111.txt','Làm tốt, bổ sung sơ đồ use case','IN_PROGRESS','Báo cáo tuần 1',NULL,4,9),(10,'kiểm tra realtime','2026-10-01 03:02:13.005384',NULL,NULL,NULL,'TODO','ws-check 1790823732915',NULL,4,9),(11,'Đã hoàn thành phân tích yêu cầu','2026-10-01 03:06:36.471551',NULL,'/api/secure/files/f8f9fb81-8399-4e6e-b392-83b751a73067.txt','Làm tốt, bổ sung sơ đồ use case','IN_PROGRESS','Báo cáo tuần 1',NULL,4,10),(12,'kiểm tra realtime','2026-10-01 03:06:47.184964',NULL,NULL,NULL,'TODO','ws-check 1790824007159',NULL,4,10),(13,'Đã hoàn thành phân tích yêu cầu','2026-10-01 03:14:04.037265',NULL,'/api/secure/files/830519aa-778e-41f7-a6f8-f83a461633e6.txt','Làm tốt, bổ sung sơ đồ use case','IN_PROGRESS','Báo cáo tuần 1',NULL,4,11),(14,'kiểm tra realtime','2026-10-01 03:14:16.374452',NULL,NULL,NULL,'TODO','ws-check 1790824456348',NULL,4,11),(15,'Đã hoàn thành phân tích yêu cầu','2026-10-01 03:23:59.907931',NULL,'/api/secure/files/880ae403-8650-48fb-be7c-e1f5558ad08a.txt','Làm tốt, bổ sung sơ đồ use case','IN_PROGRESS','Báo cáo tuần 1',NULL,4,12),(16,'kiểm tra realtime','2026-10-01 03:24:12.390809',NULL,NULL,NULL,'TODO','ws-check 1790825052363',NULL,4,12),(17,'Đã hoàn thành phân tích yêu cầu','2026-10-01 03:26:40.694504',NULL,'/api/secure/files/6cb77a4a-2e22-4358-8b4d-fc078c461291.txt','Làm tốt, bổ sung sơ đồ use case','IN_PROGRESS','Báo cáo tuần 1',NULL,4,13),(18,'kiểm tra realtime','2026-10-01 03:26:51.786487',NULL,NULL,NULL,'TODO','ws-check 1790825211751',NULL,4,13);
/*!40000 ALTER TABLE `progress_report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_permissions`
--

DROP TABLE IF EXISTS `project_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `can_create` bit(1) DEFAULT NULL,
  `can_delete` bit(1) DEFAULT NULL,
  `can_update` bit(1) DEFAULT NULL,
  `can_view` bit(1) DEFAULT NULL,
  `role_name` varchar(50) NOT NULL,
  `tool_name` varchar(50) NOT NULL,
  `topic_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKcy25kdapb4ognvkpgr9ncq5rt` (`topic_id`,`role_name`,`tool_name`),
  CONSTRAINT `FKm82wbegeg6khfjba1vs2hggw7` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_permissions`
--

LOCK TABLES `project_permissions` WRITE;
/*!40000 ALTER TABLE `project_permissions` DISABLE KEYS */;
/*!40000 ALTER TABLE `project_permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKofx66keruapi6vyqpv6f2or37` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES (1,'Quản trị viên toàn hệ thống','ADMIN'),(2,'Quản trị viên cấp khoa','DEPARTMENT_ADMIN'),(3,'Giảng viên hướng dẫn','LECTURER'),(4,'Sinh viên thực hiện đề tài','STUDENT');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles_privileges`
--

DROP TABLE IF EXISTS `roles_privileges`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles_privileges` (
  `role_id` bigint NOT NULL,
  `privilege_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`,`privilege_id`),
  KEY `FK5duhoc7rwt8h06avv41o41cfy` (`privilege_id`),
  CONSTRAINT `FK5duhoc7rwt8h06avv41o41cfy` FOREIGN KEY (`privilege_id`) REFERENCES `privileges` (`id`),
  CONSTRAINT `FK629oqwrudgp5u7tewl07ayugj` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles_privileges`
--

LOCK TABLES `roles_privileges` WRITE;
/*!40000 ALTER TABLE `roles_privileges` DISABLE KEYS */;
INSERT INTO `roles_privileges` VALUES (1,1),(1,2),(1,3),(2,3),(1,4),(2,4),(1,5),(2,5),(3,5),(1,6),(2,6);
/*!40000 ALTER TABLE `roles_privileges` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stored_files`
--

DROP TABLE IF EXISTS `stored_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stored_files` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content_type` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `original_name` varchar(255) NOT NULL,
  `owner_id` bigint NOT NULL,
  `ref_id` bigint DEFAULT NULL,
  `ref_type` varchar(30) DEFAULT NULL,
  `size_bytes` bigint NOT NULL,
  `stored_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKjjd7hf5amerajq056e2nd5y31` (`stored_name`),
  KEY `idx_stored_files_owner` (`owner_id`),
  KEY `idx_stored_files_ref` (`ref_type`,`ref_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stored_files`
--

LOCK TABLES `stored_files` WRITE;
/*!40000 ALTER TABLE `stored_files` DISABLE KEYS */;
INSERT INTO `stored_files` VALUES (1,'application/octet-stream','2026-10-01 03:06:23.363365','smoke-606573.txt',4,1,'PROGRESS_REPORT',23,'2d769377-ca55-49fe-99c9-1f684148815c_smoke-606573.txt'),(2,'application/octet-stream','2026-10-01 03:06:23.386199','smoke-658255.txt',4,2,'PROGRESS_REPORT',23,'e0f3373e-94ab-4b4d-b772-c23e0b48c7b0_smoke-658255.txt'),(3,'application/octet-stream','2026-10-01 03:06:23.393576','smoke-896348.txt',4,3,'PROGRESS_REPORT',23,'abd22052-d5a4-4007-93a0-e127afd469a2_smoke-896348.txt'),(4,'application/octet-stream','2026-10-01 03:06:23.399929','smoke-390294.txt',4,4,'PROGRESS_REPORT',23,'ac0cc914-edcb-453d-b318-1d7a993ba87e_smoke-390294.txt'),(5,'application/octet-stream','2026-10-01 03:06:23.405840','smoke-753781.txt',4,5,'PROGRESS_REPORT',23,'2e4e86b8-0293-478e-a445-a69a22e37a69_smoke-753781.txt'),(6,'application/octet-stream','2026-10-01 03:06:23.411764','smoke-922267.txt',4,6,'PROGRESS_REPORT',23,'7d92d675-96ed-47e5-86c3-fff7d6300130_smoke-922267.txt'),(7,'application/octet-stream','2026-10-01 03:06:23.418015','smoke-639680.txt',4,7,'PROGRESS_REPORT',23,'49eb57e1-ce33-4833-a4c3-0715aae5b0f0_smoke-639680.txt'),(8,'application/octet-stream','2026-10-01 03:06:23.424065','smoke-435288.txt',4,8,'PROGRESS_REPORT',23,'39a1473a-a8ce-4b9e-8af4-2df2e613bde1_smoke-435288.txt'),(9,'application/octet-stream','2026-10-01 03:06:23.431191','smoke-495111.txt',4,9,'PROGRESS_REPORT',23,'587d70ac-7531-41eb-81ef-eff0cf218f89_smoke-495111.txt'),(10,'text/plain','2026-10-01 03:06:36.402284','smoke-983992.txt',4,11,'PROGRESS_REPORT',23,'f8f9fb81-8399-4e6e-b392-83b751a73067.txt'),(11,'text/plain','2026-10-01 03:14:03.971433','smoke-429985.txt',4,13,'PROGRESS_REPORT',23,'830519aa-778e-41f7-a6f8-f83a461633e6.txt'),(12,'text/plain','2026-10-01 03:23:59.845553','smoke-027033.txt',4,15,'PROGRESS_REPORT',23,'880ae403-8650-48fb-be7c-e1f5558ad08a.txt'),(13,'text/plain','2026-10-01 03:26:40.665596','smoke-188143.txt',4,17,'PROGRESS_REPORT',23,'6cb77a4a-2e22-4358-8b4d-fc078c461291.txt');
/*!40000 ALTER TABLE `stored_files` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `students`
--

DROP TABLE IF EXISTS `students`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `students` (
  `id` bigint NOT NULL,
  `class_name` varchar(255) DEFAULT NULL,
  `student_code` varchar(255) DEFAULT NULL,
  `class_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKpe06fepkeagn2w6pppj10xf68` (`class_id`),
  CONSTRAINT `FK7xqmtv7r2eb5axni3jm0a80su` FOREIGN KEY (`id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKpe06fepkeagn2w6pppj10xf68` FOREIGN KEY (`class_id`) REFERENCES `academic_classes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `students`
--

LOCK TABLES `students` WRITE;
/*!40000 ALTER TABLE `students` DISABLE KEYS */;
INSERT INTO `students` VALUES (4,'CNTT-K15','SV001',1),(5,'CNTT-K15','SV002',1),(7,'X','X606573',NULL),(9,'X','X658255',NULL),(11,'X','X896348',NULL),(13,'X','X390294',NULL),(15,'X','X753781',NULL),(17,'X','X922267',NULL),(19,'X','X639680',NULL),(23,'X','X495111',NULL),(25,'X','X983992',NULL);
/*!40000 ALTER TABLE `students` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `topic_registration`
--

DROP TABLE IF EXISTS `topic_registration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topic_registration` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `approved` tinyint(1) DEFAULT NULL,
  `feedback` text,
  `registered_at` datetime(6) NOT NULL,
  `reject_reason` varchar(500) DEFAULT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `score` double DEFAULT NULL,
  `reviewer_id` bigint DEFAULT NULL,
  `student_id` bigint NOT NULL,
  `topic_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKgagrhg6cx8bjxs0551kc7a9vs` (`reviewer_id`),
  KEY `FK787dgaa34uitlx5231w5kyqom` (`student_id`),
  KEY `FK10j7yem7erfd8odad81rtsnw7` (`topic_id`),
  CONSTRAINT `FK10j7yem7erfd8odad81rtsnw7` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`),
  CONSTRAINT `FK787dgaa34uitlx5231w5kyqom` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `FKgagrhg6cx8bjxs0551kc7a9vs` FOREIGN KEY (`reviewer_id`) REFERENCES `lecturers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `topic_registration`
--

LOCK TABLES `topic_registration` WRITE;
/*!40000 ALTER TABLE `topic_registration` DISABLE KEYS */;
INSERT INTO `topic_registration` VALUES (1,1,'Tốt','2026-09-25 07:50:19.639724',NULL,'2026-09-25 07:50:19.800364',8.5,3,4,1),(2,NULL,NULL,'2026-09-25 07:50:19.757700',NULL,NULL,NULL,NULL,5,1),(3,1,'Tốt','2026-09-25 07:51:08.578256',NULL,'2026-09-25 07:51:08.692836',8.5,3,4,2),(4,NULL,NULL,'2026-09-25 07:51:08.655614',NULL,NULL,NULL,NULL,5,2),(5,1,'Tốt','2026-09-25 07:55:05.410208',NULL,'2026-09-25 07:55:05.548318',8.5,3,4,3),(6,NULL,NULL,'2026-09-25 07:55:05.504704',NULL,NULL,NULL,NULL,5,3),(7,1,'Tốt','2026-09-30 03:36:39.866813',NULL,'2026-09-30 03:36:40.030954',8.5,3,4,4),(8,NULL,NULL,'2026-09-30 03:36:39.970662',NULL,NULL,NULL,NULL,5,4),(9,1,'Tốt','2026-09-30 05:39:22.862363',NULL,'2026-09-30 05:39:23.027418',8.5,3,4,5),(10,NULL,NULL,'2026-09-30 05:39:22.960468',NULL,NULL,NULL,NULL,5,5),(11,1,'Tốt','2026-09-30 05:42:10.947555',NULL,'2026-09-30 05:42:11.063519',8.5,3,4,6),(12,NULL,NULL,'2026-09-30 05:42:11.025894',NULL,NULL,NULL,NULL,5,6),(13,1,'Tốt','2026-09-30 05:54:08.132970',NULL,'2026-09-30 05:54:08.298093',8.5,3,4,7),(14,NULL,NULL,'2026-09-30 05:54:08.237252',NULL,NULL,NULL,NULL,5,7),(15,1,'Tốt','2026-10-01 02:57:24.627499',NULL,'2026-10-01 02:57:24.817485',8.5,3,4,8),(16,NULL,NULL,'2026-10-01 02:57:24.747477',NULL,NULL,NULL,NULL,5,8),(17,1,'Tốt','2026-10-01 02:58:23.961935',NULL,'2026-10-01 02:58:24.143034',8.5,3,4,9),(18,NULL,NULL,'2026-10-01 02:58:24.082518',NULL,NULL,NULL,NULL,5,9),(19,1,'Tốt','2026-10-01 03:06:33.905670',NULL,'2026-10-01 03:06:34.089083',8.5,3,4,10),(20,NULL,NULL,'2026-10-01 03:06:34.030565',NULL,NULL,NULL,NULL,5,10),(21,1,'Tốt','2026-10-01 03:14:01.745895',NULL,'2026-10-01 03:14:01.935843',8.5,3,4,11),(22,NULL,NULL,'2026-10-01 03:14:01.882850',NULL,NULL,NULL,NULL,5,11),(23,1,'Tốt','2026-10-01 03:23:57.518868',NULL,'2026-10-01 03:23:57.706653',8.5,3,4,12),(24,NULL,NULL,'2026-10-01 03:23:57.653096',NULL,NULL,NULL,NULL,5,12),(25,1,'Tốt','2026-10-01 03:26:38.505991',NULL,'2026-10-01 03:26:38.656631',8.5,3,4,13),(26,NULL,NULL,'2026-10-01 03:26:38.613368',NULL,NULL,NULL,NULL,5,13);
/*!40000 ALTER TABLE `topic_registration` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `topics`
--

DROP TABLE IF EXISTS `topics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topics` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `capacity` int DEFAULT NULL,
  `description` text,
  `status` varchar(50) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `lecturer_id` bigint DEFAULT NULL,
  `workspace_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK2rrx0i1r6ecobj5uuu4t83p6m` (`lecturer_id`),
  KEY `FK20bsyno5jeliiync88nsj7v0d` (`workspace_id`),
  CONSTRAINT `FK20bsyno5jeliiync88nsj7v0d` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`),
  CONSTRAINT `FK2rrx0i1r6ecobj5uuu4t83p6m` FOREIGN KEY (`lecturer_id`) REFERENCES `lecturers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `topics`
--

LOCK TABLES `topics` WRITE;
/*!40000 ALTER TABLE `topics` DISABLE KEYS */;
INSERT INTO `topics` VALUES (1,3,'Kiểm thử tự động','COMPLETED','Đề tài smoke 606573',3,2),(2,3,'Kiểm thử tự động','COMPLETED','Đề tài smoke 658255',3,3),(3,3,'Kiểm thử tự động','COMPLETED','Đề tài smoke 896348',3,4),(4,3,'Kiểm thử tự động','COMPLETED','Đề tài smoke 390294',3,5),(5,3,'Kiểm thử tự động','COMPLETED','Đề tài smoke 753781',3,6),(6,3,'Kiểm thử tự động','COMPLETED','Đề tài smoke 922267',3,7),(7,3,'Kiểm thử tự động','COMPLETED','Đề tài smoke 639680',3,8),(8,3,'Kiểm thử tự động','COMPLETED','Đề tài smoke 435288',3,9),(9,3,'Kiểm thử tự động','COMPLETED','Đề tài smoke 495111',3,10),(10,3,'Kiểm thử tự động','COMPLETED','Đề tài smoke 983992',3,11),(11,3,'Kiểm thử tự động','COMPLETED','Đề tài smoke 429985',3,12),(12,3,'Kiểm thử tự động','COMPLETED','Đề tài smoke 027033',3,13),(13,3,'Kiểm thử tự động','COMPLETED','Đề tài smoke 188143',3,14);
/*!40000 ALTER TABLE `topics` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `full_name` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `username` varchar(50) NOT NULL,
  `department_id` bigint DEFAULT NULL,
  `role_id` bigint NOT NULL,
  `activated` bit(1) NOT NULL DEFAULT b'1',
  `must_change_password` bit(1) NOT NULL DEFAULT b'0',
  `token_version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  KEY `FKsbg59w8q63i0oo53rlgvlcnjq` (`department_id`),
  KEY `FKp56c1712k691lhsyewcssf40f` (`role_id`),
  CONSTRAINT `FKp56c1712k691lhsyewcssf40f` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `FKsbg59w8q63i0oo53rlgvlcnjq` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,_binary '','admin@example.com','Quản trị hệ thống','$2a$10$IUAS2mkh0PBY.sZwLsWYcekMTxBtEDs3rp2UU8KYoQYqO3aXIXT66',NULL,'admin',NULL,1,_binary '',_binary '\0',0),(2,_binary '','deptadmin@example.com','Quản trị khoa CNTT','$2a$10$/OPh1kIzY7zZR2oQynaMU.8tnAxXaATsUtvMKva.Ae.zMfwzRew/u',NULL,'deptadmin',1,2,_binary '',_binary '\0',0),(3,_binary '','lecturer@example.com','ThS. Giảng viên Demo','$2a$10$3CvrnI97/u/ffRJ2mZ/ypu2NwNioHdnvq4MDWh57iRZnqF78/7Xh2',NULL,'lecturer',1,3,_binary '',_binary '\0',0),(4,_binary '','student@example.com','Sinh viên Demo','$2a$10$6LoCeYam9eWfJKuYqMFeY.ki3J4jxumkwhfUSzcnYkr4/LNxh.28S',NULL,'student',1,4,_binary '',_binary '\0',15),(5,_binary '','student2@example.com','Sinh viên Demo 2','$2a$10$DiqOwJ2PhAAuovGx1.aGKuxRpmzEZqWis9swHVYtKWmhgLMnOmEb6',NULL,'student2',1,4,_binary '',_binary '\0',0),(6,_binary '',NULL,'GV thử 606573','$2a$10$OFjnVs/P5VpgCzbG94UDI.hHI33TVEvuzK096/ZUG/CGUxw8NytZ2',NULL,'lect606573',1,3,_binary '',_binary '\0',0),(7,_binary '',NULL,'Tự đăng ký','$2a$10$wi.MQpshqc7UL0gDelT7OO8p6lvF2xwtQO2WUaG2pV7lGi6fohTzi',NULL,'self606573',NULL,4,_binary '',_binary '\0',0),(8,_binary '',NULL,'GV thử 658255','$2a$10$tyb8P7ekB/3wbDi6/siY8.CPbvf2QxD4Bv3ghcWndxy9tfdAZeBx.',NULL,'lect658255',1,3,_binary '',_binary '\0',0),(9,_binary '',NULL,'Tự đăng ký','$2a$10$wy4i5QqIgo66OVxP.Uk3heb0VlkH/Ss5o.VXOYipSunfo.O.CufnS',NULL,'self658255',NULL,4,_binary '',_binary '\0',0),(10,_binary '',NULL,'GV thử 896348','$2a$10$5M8NDbC60oWxftIw0qAkbumftJ4cLZzBnkLUQCYy4e7vwBArLjAEO',NULL,'lect896348',1,3,_binary '',_binary '\0',0),(11,_binary '',NULL,'Tự đăng ký','$2a$10$8Y.uhfWZLoTtHvlgAbAlcuo1p0smoJxBcD5pmPMIjYo0W9piLJ1cG',NULL,'self896348',NULL,4,_binary '',_binary '\0',0),(12,_binary '',NULL,'GV thử 390294','$2a$10$Nso0EjWwuJZ/DzBtJUYRauuRdyOjAx2vHIz/bhO13ZzRYFFjnqZ2W',NULL,'lect390294',1,3,_binary '',_binary '\0',0),(13,_binary '',NULL,'Tự đăng ký','$2a$10$mVxS7HTQt.23Zjxv67YkKOLclXokHk6ubSSc6WJO3RkbIEbljlJGq',NULL,'self390294',NULL,4,_binary '',_binary '\0',0),(14,_binary '',NULL,'GV thử 753781','$2a$10$9fPk6mjdZGibc503r.9Jz.K4Q/IJ6K9h6RszL3gKLEB8eNZQaC0cq',NULL,'lect753781',1,3,_binary '',_binary '\0',0),(15,_binary '',NULL,'Tự đăng ký','$2a$10$TBFkK6hefUJYEpND6IsrDOLSt2PYzTVLwJIo.T4XJ8ALPzVMhoIEe',NULL,'self753781',NULL,4,_binary '',_binary '\0',0),(16,_binary '',NULL,'GV thử 922267','$2a$10$vM5OI8FDS9WZWAxdmlhZL.VFqBLYC.8NvikhrgKohsLAQ74wRrMcC',NULL,'lect922267',1,3,_binary '',_binary '\0',0),(17,_binary '',NULL,'Tự đăng ký','$2a$10$A.FcJXHWajG/JU0F1giqE.af7FtiyLBOs0DXWZ.OHgtq/4nqFYl4m',NULL,'self922267',NULL,4,_binary '',_binary '\0',0),(18,_binary '',NULL,'GV thử 639680','$2a$10$RE.ZxCjU7OyeWPy/dQtnPeSqKUaI0uE.TrU1J5fonI9ShrMSPNKp6',NULL,'lect639680',1,3,_binary '',_binary '\0',0),(19,_binary '',NULL,'Tự đăng ký','$2a$10$fhOGc338juY/.h09r3SEVOFHz8LZsjQnYjZNwM5dskwOMN5cKqXhe',NULL,'self639680',NULL,4,_binary '',_binary '\0',0),(20,_binary '',NULL,'GV thử 435288','$2a$10$LUK2daMB949/jfJk/99Z8.CAjQ./.bejYe4wDRGXdGULnHCeNd4d2',NULL,'lect435288',1,3,_binary '',_binary '\0',0),(21,_binary '',NULL,'Tự đăng ký','$2a$10$qvtos.cQqXLmnXU/bSrWmOgYkL1ASmL1RsU9cD5RlsA9BP9cWWchi',NULL,'self435288',NULL,4,_binary '',_binary '\0',0),(22,_binary '',NULL,'GV thử 495111','$2a$10$hcQ9tHPxK.L/NSMqhOk96.x/OQkIGjvpP0Vv0nzSl9IlVxMwLW9Hi',NULL,'lect495111',1,3,_binary '',_binary '\0',0),(23,_binary '',NULL,'Tự đăng ký','$2a$10$M8YpvyUVRiiMwh2T.vz71eCFiX476keKvYF8wM6OrJSrR9pENRx0.',NULL,'self495111',NULL,4,_binary '',_binary '\0',0),(24,_binary '',NULL,'GV thử 983992','$2a$10$UivhCEVL6jgZP7zfJ8rYwONSHuBwSm.JRPt65sdraCz/ORz0oNQYS',NULL,'lect983992',1,3,_binary '',_binary '\0',0),(25,_binary '',NULL,'Tự đăng ký','$2a$10$jejabk5Bve5fJ6.zt.1DFeu35TL99tpa3ZHqn.dZUc.ZuWekXCgja',NULL,'self983992',NULL,4,_binary '',_binary '\0',0),(26,_binary '','lect429985@smoke.local','GV thử 429985','$2a$10$CrB0brzH6KJAnFI/73ihX.y4WMXkswwILDw.efWZ2P5C7Y0bRtdi6',NULL,'lect429985',1,3,_binary '',_binary '\0',1),(27,_binary '','lect027033@smoke.local','GV thử 027033','$2a$10$Rr7kmaYh8fG7ZQzut1mZ2eg16dmiUjFjIOg2GJpl2xr4tNCvFhqwa',NULL,'lect027033',1,3,_binary '',_binary '\0',1),(28,_binary '','lect188143@smoke.local','GV thử 188143','$2a$10$m8dJEkT6sSyAFSy9rzZhNe.UJrjrAt/DJO9A/JzpBVa4OqpeKNthy',NULL,'lect188143',1,3,_binary '',_binary '\0',1);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `workspace_classes`
--

DROP TABLE IF EXISTS `workspace_classes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `workspace_classes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `class_id` bigint NOT NULL,
  `workspace_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9rb4ruifhhpprs2s8dh6q93jj` (`workspace_id`,`class_id`),
  KEY `FKpcjqw35ttovnlvvh1n2pou4ul` (`class_id`),
  CONSTRAINT `FKpcjqw35ttovnlvvh1n2pou4ul` FOREIGN KEY (`class_id`) REFERENCES `academic_classes` (`id`),
  CONSTRAINT `FKrywthrafxc1ya467dodhrybn6` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `workspace_classes`
--

LOCK TABLES `workspace_classes` WRITE;
/*!40000 ALTER TABLE `workspace_classes` DISABLE KEYS */;
INSERT INTO `workspace_classes` VALUES (1,_binary '',1,2),(2,_binary '',1,3),(3,_binary '',1,4),(4,_binary '',1,5),(5,_binary '',1,6),(6,_binary '',1,7),(7,_binary '',1,8),(8,_binary '',1,9),(9,_binary '',1,10),(10,_binary '',1,11),(11,_binary '',1,12),(12,_binary '',1,13),(13,_binary '',1,14);
/*!40000 ALTER TABLE `workspace_classes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `workspaces`
--

DROP TABLE IF EXISTS `workspaces`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `workspaces` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `end_at` datetime(6) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `semester` varchar(50) DEFAULT NULL,
  `start_at` datetime(6) DEFAULT NULL,
  `status` varchar(30) NOT NULL,
  `type` varchar(50) DEFAULT NULL,
  `department_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmy77je4f9pwmh064o2bqu07s2` (`department_id`),
  CONSTRAINT `FKmy77je4f9pwmh064o2bqu07s2` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `workspaces`
--

LOCK TABLES `workspaces` WRITE;
/*!40000 ALTER TABLE `workspaces` DISABLE KEYS */;
INSERT INTO `workspaces` VALUES (1,_binary '',NULL,'Đồ án CNPM HK1 2026-2027','HK1 2026-2027',NULL,'DRAFT','DO_AN',1),(2,_binary '',NULL,'Smoke 606573','HK1',NULL,'IN_PROGRESS','DO_AN',1),(3,_binary '',NULL,'Smoke 658255','HK1',NULL,'IN_PROGRESS','DO_AN',1),(4,_binary '',NULL,'Smoke 896348','HK1',NULL,'IN_PROGRESS','DO_AN',1),(5,_binary '',NULL,'Smoke 390294','HK1',NULL,'IN_PROGRESS','DO_AN',1),(6,_binary '',NULL,'Smoke 753781','HK1',NULL,'IN_PROGRESS','DO_AN',1),(7,_binary '',NULL,'Smoke 922267','HK1',NULL,'IN_PROGRESS','DO_AN',1),(8,_binary '',NULL,'Smoke 639680','HK1',NULL,'IN_PROGRESS','DO_AN',1),(9,_binary '',NULL,'Smoke 435288','HK1',NULL,'IN_PROGRESS','DO_AN',1),(10,_binary '',NULL,'Smoke 495111','HK1',NULL,'IN_PROGRESS','DO_AN',1),(11,_binary '',NULL,'Smoke 983992','HK1',NULL,'IN_PROGRESS','DO_AN',1),(12,_binary '',NULL,'Smoke 429985','HK1',NULL,'IN_PROGRESS','DO_AN',1),(13,_binary '',NULL,'Smoke 027033','HK1',NULL,'IN_PROGRESS','DO_AN',1),(14,_binary '',NULL,'Smoke 188143','HK1',NULL,'IN_PROGRESS','DO_AN',1);
/*!40000 ALTER TABLE `workspaces` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-23  6:50:12
