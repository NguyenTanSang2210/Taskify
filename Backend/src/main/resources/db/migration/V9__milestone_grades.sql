-- P4-05: trọng số điểm của từng mốc (%) và điểm của sinh viên theo từng mốc.
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'milestones' AND column_name = 'weight') = 0, 'ALTER TABLE `milestones` ADD COLUMN `weight` int NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `milestone_grades` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `milestone_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `score` double NOT NULL,
  `feedback` text,
  `graded_by_id` bigint DEFAULT NULL,
  `graded_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_milestone_grade` (`milestone_id`, `student_id`),
  KEY `idx_milestone_grade_student` (`student_id`),
  CONSTRAINT `fk_milestone_grade_milestone` FOREIGN KEY (`milestone_id`) REFERENCES `milestones` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_milestone_grade_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `fk_milestone_grade_grader` FOREIGN KEY (`graded_by_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
