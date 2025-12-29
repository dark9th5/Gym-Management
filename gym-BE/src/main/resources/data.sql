-- Reset guidance tables each startup (development only)
SET FOREIGN_KEY_CHECKS=0;
TRUNCATE TABLE guidance_lesson_details;
TRUNCATE TABLE guidance_lessons;
TRUNCATE TABLE guidance_categories;
SET FOREIGN_KEY_CHECKS=1;

-- Guidance Categories
INSERT IGNORE INTO guidance_categories (name, display_name) VALUES
('chest', 'Ngực'),
('shoulders', 'Vai'),
('triceps', 'Tay sau'),
('back', 'Lưng'),
('abs', 'Bụng'),
('biceps', 'Tay trước'),
('legs', 'Chân');

-- Guidance Lessons for Chest (category_id = 1)
INSERT IGNORE INTO guidance_lessons (title, description, lesson_number, category_id) VALUES
('Bài tập đẩy ngực với tạ đòn', 'Hướng dẫn cơ bản đẩy ngực để phát triển cơ ngực lớn.', 1, 1),
('Bài tập đẩy ngực trên máy', 'Sử dụng máy đẩy ngực để tập trung vào cơ ngực giữa.', 2, 1),
('Bài tập butterfly máy', 'Bài tập mở rộng ngực với máy butterfly.', 3, 1),
('Bài tập đẩy ngực nghiêng', 'Đẩy ngực nghiêng để phát triển phần trên ngực.', 4, 1),
('Bài tập crossover cáp', 'Sử dụng cáp để kéo chéo, tập trung vào ngực trong.', 5, 1),
('Bài tập đẩy ngực với dumbbell', 'Đẩy ngực với tạ đơn để cân bằng hai bên.', 6, 1),
('Bài tập chest press', 'Bài tập trên máy chest press cho sức mạnh.', 7, 1),
('Bài tập push-up', 'Bài tập push-up cơ bản không cần dụng cụ.', 8, 1);

-- Guidance Lessons for Shoulders (category_id = 2)
INSERT IGNORE INTO guidance_lessons (title, description, lesson_number, category_id) VALUES
('Bài tập nâng vai với dumbbell', 'Nâng vai đứng để phát triển cơ vai trước.', 1, 2),
('Bài tập military press', 'Đẩy vai trên với tạ đòn.', 2, 2),
('Bài tập lateral raise', 'Nâng vai ngang để mở rộng vai.', 3, 2),
('Bài tập rear delt fly', 'Bay vai sau với dumbbell.', 4, 2),
('Bài tập shoulder press máy', 'Đẩy vai trên máy.', 5, 2),
('Bài tập upright row', 'Kéo vai đứng với tạ.', 6, 2),
('Bài tập front raise', 'Nâng vai trước với dumbbell.', 7, 2),
('Bài tập shrug', 'Nhún vai với tạ để phát triển bắp vai.', 8, 2);

-- Guidance Lessons for Triceps (category_id = 3)
INSERT IGNORE INTO guidance_lessons (title, description, lesson_number, category_id) VALUES
('Bài tập dips', 'Chống đẩy tay sau trên ghế.', 1, 3),
('Bài tập tricep extension', 'Duỗi tay sau với dumbbell.', 2, 3),
('Bài tập skull crushers', 'Đè đầu với tạ đòn.', 3, 3),
('Bài tập tricep kickback', 'Đá tay sau với dumbbell.', 4, 3),
('Bài tập close-grip bench press', 'Đẩy ngực nắm chặt tay.', 5, 3),
('Bài tập overhead tricep extension', 'Duỗi tay trên đầu.', 6, 3),
('Bài tập tricep rope pushdown', 'Kéo dây cáp tay sau.', 7, 3),
('Bài tập diamond push-up', 'Push-up dạng kim cương.', 8, 3);

-- Guidance Lessons for Back (category_id = 4)
INSERT IGNORE INTO guidance_lessons (title, description, lesson_number, category_id) VALUES
('Bài tập deadlift', 'Kéo tạ chết để phát triển lưng dưới.', 1, 4),
('Bài tập pull-up', 'Kéo người lên xà.', 2, 4),
('Bài tập bent-over row', 'Kéo tạ nghiêng.', 3, 4),
('Bài tập lat pulldown', 'Kéo xô máy.', 4, 4),
('Bài tập seated row', 'Kéo ngồi máy.', 5, 4),
('Bài tập face pull', 'Kéo mặt với cáp.', 6, 4),
('Bài tập T-bar row', 'Kéo T-bar.', 7, 4),
('Bài tập hyperextension', 'Duỗi lưng trên máy.', 8, 4);

-- Guidance Lessons for Abs (category_id = 5)
INSERT IGNORE INTO guidance_lessons (title, description, lesson_number, category_id) VALUES
('Bài tập crunch', 'Cuốn bụng cơ bản.', 1, 5),
('Bài tập plank', 'Giữ plank để tăng sức bền bụng.', 2, 5),
('Bài tập leg raise', 'Nâng chân nằm.', 3, 5),
('Bài tập Russian twist', 'Xoay người Nga.', 4, 5),
('Bài tập bicycle crunch', 'Cuốn xe đạp.', 5, 5),
('Bài tập mountain climber', 'Leo núi.', 6, 5),
('Bài tập sit-up', 'Ngồi dậy.', 7, 5),
('Bài tập flutter kick', 'Đạp chân bướm.', 8, 5);

-- Guidance Lessons for Biceps (category_id = 6)
INSERT IGNORE INTO guidance_lessons (title, description, lesson_number, category_id) VALUES
('Bài tập bicep curl', 'Cuốn tay trước với dumbbell.', 1, 6),
('Bài tập hammer curl', 'Cuốn búa với dumbbell.', 2, 6),
('Bài tập preacher curl', 'Cuốn trên máy preacher.', 3, 6),
('Bài tập concentration curl', 'Cuốn tập trung.', 4, 6),
('Bài tập barbell curl', 'Cuốn tạ đòn.', 5, 6),
('Bài tập cable curl', 'Cuốn cáp.', 6, 6),
('Bài tập chin-up', 'Kéo cằm lên xà.', 7, 6),
('Bài tập reverse curl', 'Cuốn ngược.', 8, 6);

-- Guidance Lessons for Legs (category_id = 7)
INSERT IGNORE INTO guidance_lessons (title, description, lesson_number, category_id) VALUES
('Bài tập squat', 'Bài tập squat cơ bản.', 1, 7),
('Bài tập leg press', 'Đạp chân máy.', 2, 7),
('Bài tập lunges', 'Chạy bước.', 3, 7),
('Bài tập leg extension', 'Duỗi chân máy.', 4, 7),
('Bài tập leg curl', 'Cuốn chân máy.', 5, 7),
('Bài tập calf raise', 'Nâng bắp chân.', 6, 7),
('Bài tập step-up', 'Bước lên.', 7, 7),
('Bài tập deadlift chân', 'Deadlift tập trung chân.', 8, 7);

-- Guidance Lesson Details (mỗi lesson có 1 detail)
INSERT IGNORE INTO guidance_lesson_details (content, video_url, image_url, lesson_id) VALUES
-- Chest lessons (Ngực)
-- Lưu ý: Bài đầu tiên mô tả "Đứng thẳng, đẩy tạ qua đầu" là bài vai (Overhead Press), không phải ngực. Tôi đã đổi thành Barbell Bench Press cho đúng nhóm cơ Ngực.
('Nằm ghế phẳng, đẩy tạ đòn (Barbell Bench Press). 3 set x 10-12 reps. Giữ lưng thẳng, hít thở đều.', 'https://www.youtube.com/watch?v=rT7DgCr-3pg', 'https://img.youtube.com/vi/rT7DgCr-3pg/hqdefault.jpg', 1),
('Ngồi máy Chest Press, đẩy tay về phía trước. Điều chỉnh ghế ngồi ngang ngực. 3 set x 12 reps.', 'https://www.youtube.com/watch?v=xUm0rrCZ7KQ', 'https://img.youtube.com/vi/xUm0rrCZ7KQ/hqdefault.jpg', 2),
('Ngồi máy Pec Deck (Butterfly), ép ngực. Tập trung vào đỉnh co cơ. 3 set x 15 reps.', 'https://www.youtube.com/watch?v=O-OnOBCg19I', 'https://img.youtube.com/vi/O-OnOBCg19I/hqdefault.jpg', 3),
('Nằm ghế dốc lên (Incline Dumbbell Press). Phát triển phần ngực trên. 3 set x 10 reps.', 'https://www.youtube.com/watch?v=8iPEnn-ltC8', 'https://img.youtube.com/vi/8iPEnn-ltC8/hqdefault.jpg', 4),
('Đứng kéo cáp chéo từ trên xuống (High Cable Fly). Ép ngực dưới/giữa. 3 set x 12 reps.', 'https://www.youtube.com/watch?v=l41SoIxDXc0', 'https://img.youtube.com/vi/l41SoIxDXc0/hqdefault.jpg', 5),
('Nằm ghế phẳng, đẩy tạ đơn (Dumbbell Bench Press). Tăng phạm vi chuyển động. 3 set x 10 reps.', 'https://www.youtube.com/watch?v=VmB1G1K7v94', 'https://img.youtube.com/vi/VmB1G1K7v94/hqdefault.jpg', 6),
('Hít đất cơ bản (Push-up). Giữ người thẳng như tấm ván. 3 set x 15 reps.', 'https://www.youtube.com/watch?v=IODxDxX7oi4', 'https://img.youtube.com/vi/IODxDxX7oi4/hqdefault.jpg', 7),
('Hít đất kim cương (Diamond Push-up). Tay chụm lại hình kim cương. 3 set x 12 reps.', 'https://www.youtube.com/watch?v=J0DnG1_S92I', 'https://img.youtube.com/vi/J0DnG1_S92I/hqdefault.jpg', 8),

-- Shoulders lessons (Vai)
('Đứng nâng tạ đơn sang ngang (Lateral Raise). Tập vai giữa. 3 set x 12 reps.', 'https://www.youtube.com/watch?v=3VcKaXpzqRo', 'https://img.youtube.com/vi/3VcKaXpzqRo/hqdefault.jpg', 9),
('Ngồi đẩy tạ đòn qua đầu (Seated Barbell Overhead Press). Tập vai trước và giữa. 3 set x 10 reps.', 'https://www.youtube.com/watch?v=2yjwXTZQDDI', 'https://img.youtube.com/vi/2yjwXTZQDDI/hqdefault.jpg', 10),
('Đứng kéo cáp sang ngang (Cable Lateral Raise). Duy trì áp lực liên tục. 3 set x 15 reps.', 'https://www.youtube.com/watch?v=PMqRMqpKvqs', 'https://img.youtube.com/vi/PMqRMqpKvqs/hqdefault.jpg', 11),
('Cúi người nâng tạ sang ngang (Bent-over Dumbbell Reverse Fly). Tập vai sau. 3 set x 12 reps.', 'https://www.youtube.com/watch?v=ttvfGg9d76c', 'https://img.youtube.com/vi/ttvfGg9d76c/hqdefault.jpg', 12),
('Ngồi máy đẩy vai (Machine Shoulder Press). An toàn cho người mới. 3 set x 10 reps.', 'https://www.youtube.com/watch?v=WvLMauqrnK8', 'https://img.youtube.com/vi/WvLMauqrnK8/hqdefault.jpg', 13),
('Kéo tạ thẳng đứng (Upright Row). Tập cầu vai và vai giữa. 3 set x 12 reps.', 'https://www.youtube.com/watch?v=amCU-ziHITM', 'https://img.youtube.com/vi/amCU-ziHITM/hqdefault.jpg', 14),
('Nâng tạ đơn ra trước (Dumbbell Front Raise). Tập vai trước. 3 set x 12 reps.', 'https://www.youtube.com/watch?v=-t7fuZ0KhDA', 'https://img.youtube.com/vi/-t7fuZ0KhDA/hqdefault.jpg', 15),
('Cầm tạ nhún vai (Dumbbell Shrug). Tập cơ thang (cầu vai). 3 set x 15 reps.', 'https://www.youtube.com/watch?v=g6qbq4Jf1_g', 'https://img.youtube.com/vi/g6qbq4Jf1_g/hqdefault.jpg', 16),

-- Triceps lessons (Tay sau)
('Nhún tay sau trên ghế (Bench Dip). Hạ người sâu vừa phải. 3 set x 10 reps.', 'https://www.youtube.com/watch?v=0326dy_-CzM', 'https://img.youtube.com/vi/0326dy_-CzM/hqdefault.jpg', 17),
('Cúi người duỗi tay sau (Tricep Kickback). Khóa cùi chỏ cố định. 3 set x 12 reps.', 'https://www.youtube.com/watch?v=6SS6K3lAwZ8', 'https://img.youtube.com/vi/6SS6K3lAwZ8/hqdefault.jpg', 18),
('Nằm ghế duỗi tay sau (Skull Crushers). Cẩn thận tạ rơi vào trán. 3 set x 10 reps.', 'https://www.youtube.com/watch?v=d_KZxkY_0cM', 'https://img.youtube.com/vi/d_KZxkY_0cM/hqdefault.jpg', 19),
('Đứng kéo cáp thừng (Rope Pushdown). Tách dây ở điểm cuối. 3 set x 12 reps.', 'https://www.youtube.com/watch?v=vB5OHsJ3EME', 'https://img.youtube.com/vi/vB5OHsJ3EME/hqdefault.jpg', 20),
('Đẩy ngực tay hẹp (Close Grip Bench Press). Tập trung vào tay sau. 3 set x 10 reps.', 'https://www.youtube.com/watch?v=nEF0bv2FW94', 'https://img.youtube.com/vi/nEF0bv2FW94/hqdefault.jpg', 21),
('Ngồi duỗi tạ sau đầu (Overhead Dumbbell Extension). Kéo giãn tay sau tối đa. 3 set x 12 reps.', 'https://www.youtube.com/watch?v=YbX7Wd8jQ-Q', 'https://img.youtube.com/vi/YbX7Wd8jQ-Q/hqdefault.jpg', 22),
('Kéo cáp tay ngược (Reverse Grip Tricep Pushdown). 3 set x 15 reps.', 'https://www.youtube.com/watch?v=gwlK2Z093f0', 'https://img.youtube.com/vi/gwlK2Z093f0/hqdefault.jpg', 23),
('Hít đất tay hẹp (Close Grip Push-up). Khép nách vào thân người. 3 set x 12 reps.', 'https://www.youtube.com/watch?v=8sLNbO3a0wU', 'https://img.youtube.com/vi/8sLNbO3a0wU/hqdefault.jpg', 24),

-- Back lessons (Lưng/Xô)
('Deadlift truyền thống. Kéo tạ từ sàn, giữ lưng thẳng tuyệt đối. 3 set x 8 reps.', 'https://www.youtube.com/watch?v=op9kVnSso6Q', 'https://img.youtube.com/vi/op9kVnSso6Q/hqdefault.jpg', 25),
('Hít xà đơn (Pull-up). Kéo cằm qua xà, tay rộng. 3 set x 10 reps.', 'https://www.youtube.com/watch?v=eGo4IYlbE5g', 'https://img.youtube.com/vi/eGo4IYlbE5g/hqdefault.jpg', 26),
('Gập người kéo tạ đòn (Barbell Bent Over Row). Kéo về phía hông. 3 set x 10 reps.', 'https://www.youtube.com/watch?v=9efgcGunU90', 'https://img.youtube.com/vi/9efgcGunU90/hqdefault.jpg', 27),
('Ngồi máy kéo xô (Lat Pulldown). Ngả người nhẹ, kéo về ngực trên. 3 set x 12 reps.', 'https://www.youtube.com/watch?v=CAwf7n6Luuc', 'https://img.youtube.com/vi/CAwf7n6Luuc/hqdefault.jpg', 28),
('Ngồi chèo cáp (Seated Cable Row). Giữ lưng thẳng, ép bả vai. 3 set x 12 reps.', 'https://www.youtube.com/watch?v=GZbfZ033f74', 'https://img.youtube.com/vi/GZbfZ033f74/hqdefault.jpg', 29),
('Kéo dây mặt (Face Pull). Tập vai sau và cơ chóp xoay. 3 set x 15 reps.', 'https://www.youtube.com/watch?v=V8dZqdIln9I', 'https://img.youtube.com/vi/V8dZqdIln9I/hqdefault.jpg', 30),
('Kéo tạ chữ T (T-Bar Row). Cố định hông, kéo tạ lên. 3 set x 10 reps.', 'https://www.youtube.com/watch?v=j3Igk5nyZE4', 'https://img.youtube.com/vi/j3Igk5nyZE4/hqdefault.jpg', 31),
('Duỗi lưng dưới (Hyperextension). Tập cơ lưng dưới. 3 set x 15 reps.', 'https://www.youtube.com/watch?v=ph3pddcUgfM', 'https://img.youtube.com/vi/ph3pddcUgfM/hqdefault.jpg', 32),

-- Abs lessons (Bụng)
('Gập bụng cơ bản (Crunch). Không dùng tay kéo cổ. 3 set x 20 reps.', 'https://www.youtube.com/watch?v=Xyd_fa5zoEU', 'https://img.youtube.com/vi/Xyd_fa5zoEU/hqdefault.jpg', 33),
('Plank (Tấm ván). Siết chặt bụng, giữ lưng thẳng 30-60s. 3 set.', 'https://www.youtube.com/watch?v=pSHjTRCQxIw', 'https://img.youtube.com/vi/pSHjTRCQxIw/hqdefault.jpg', 34),
('Nằm nâng chân (Leg Raise). Tập bụng dưới. 3 set x 15 reps.', 'https://www.youtube.com/watch?v=l4kQd9eWclE', 'https://img.youtube.com/vi/l4kQd9eWclE/hqdefault.jpg', 35),
('Ngồi xoay người kiểu Nga (Russian Twist). Cầm tạ xoay eo. 3 set x 20 reps.', 'https://www.youtube.com/watch?v=wkD8rjkodUI', 'https://img.youtube.com/vi/wkD8rjkodUI/hqdefault.jpg', 36),
('Gập bụng đạp xe (Bicycle Crunch). Chạm khuỷu tay vào gối đối diện. 3 set x 20 reps.', 'https://www.youtube.com/watch?v=IwyvZENrbG8', 'https://img.youtube.com/vi/IwyvZENrbG8/hqdefault.jpg', 37),
('Leo núi (Mountain Climber). Kéo gối lên ngực nhanh. 3 set x 30 giây.', 'https://www.youtube.com/watch?v=nmwgirgXLIg', 'https://img.youtube.com/vi/nmwgirgXLIg/hqdefault.jpg', 38),
('Gập bụng hết biên độ (Sit-up). Ngồi dậy hoàn toàn. 3 set x 15 reps.', 'https://www.youtube.com/watch?v=1fbU_MkV7NE', 'https://img.youtube.com/vi/1fbU_MkV7NE/hqdefault.jpg', 39),
('Nằm đá chân cắt kéo (Flutter Kicks). Giữ chân thẳng. 3 set x 30 giây.', 'https://www.youtube.com/watch?v=ANVdMDaYRts', 'https://img.youtube.com/vi/ANVdMDaYRts/hqdefault.jpg', 40),

-- Biceps lessons (Tay trước)
('Cuốn tạ đơn đứng (Standing Dumbbell Curl). Xoay cổ tay khi lên. 3 set x 12 reps.', 'https://www.youtube.com/watch?v=sAq_ocpRh_I', 'https://img.youtube.com/vi/sAq_ocpRh_I/hqdefault.jpg', 41),
('Cuốn tạ kiểu búa (Hammer Curl). Cầm tạ dọc, tập cẳng tay và tay trước. 3 set x 12 reps.', 'https://www.youtube.com/watch?v=zC3nLlEvin4', 'https://img.youtube.com/vi/zC3nLlEvin4/hqdefault.jpg', 42),
('Cuốn tạ trên ghế dốc (Incline Dumbbell Curl). Kéo giãn cơ bắp tay dài. 3 set x 10 reps.', 'https://www.youtube.com/watch?v=soxrZlIl35U', 'https://img.youtube.com/vi/soxrZlIl35U/hqdefault.jpg', 43),
('Ngồi cuốn tạ tập trung (Concentration Curl). Tựa tay vào đùi trong. 3 set x 12 reps.', 'https://www.youtube.com/watch?v=0AUGkch3tzc', 'https://img.youtube.com/vi/0AUGkch3tzc/hqdefault.jpg', 44),
('Cuốn thanh tạ đòn (Barbell Curl). Giữ lưng thẳng, không đung đưa. 3 set x 10 reps.', 'https://www.youtube.com/watch?v=kwG2ipFRgfo', 'https://img.youtube.com/vi/kwG2ipFRgfo/hqdefault.jpg', 45),
('Kéo cáp cuốn tay (Cable Bicep Curl). Áp lực liên tục. 3 set x 12 reps.', 'https://www.youtube.com/watch?v=AsAVryptbl4', 'https://img.youtube.com/vi/AsAVryptbl4/hqdefault.jpg', 46),
('Hít xà tay ngược (Chin-up). Lòng bàn tay hướng vào mặt. 3 set x 8 reps.', 'https://www.youtube.com/watch?v=brhRXlOhsAM', 'https://img.youtube.com/vi/brhRXlOhsAM/hqdefault.jpg', 47),
('Cuốn tạ đòn tay ngược (Reverse Barbell Curl). Tập cẳng tay và bắp tay ngoài. 3 set x 12 reps.', 'https://www.youtube.com/watch?v=nRgxYX2Ve9w', 'https://img.youtube.com/vi/nRgxYX2Ve9w/hqdefault.jpg', 48),

-- Legs lessons (Chân/Mông)
('Gánh tạ (Barbell Squat). Xuống sâu, đầu gối mở theo mũi chân. 3 set x 10 reps.', 'https://www.youtube.com/watch?v=bEv6CCg2BC8', 'https://img.youtube.com/vi/bEv6CCg2BC8/hqdefault.jpg', 49),
('Đạp đùi (Leg Press). Không khóa khớp gối khi đẩy thẳng. 3 set x 12 reps.', 'https://www.youtube.com/watch?v=IZxyjW7MPJQ', 'https://img.youtube.com/vi/IZxyjW7MPJQ/hqdefault.jpg', 50),
('Chùng chân (Dumbbell Lunge). Bước dài, hạ gối vuông góc. 3 set x 10 reps mỗi chân.', 'https://www.youtube.com/watch?v=D7KaRcUTQeE', 'https://img.youtube.com/vi/D7KaRcUTQeE/hqdefault.jpg', 51),
('Đá đùi trước (Leg Extension). Siết chặt cơ đùi ở điểm cao nhất. 3 set x 15 reps.', 'https://www.youtube.com/watch?v=YyvSfVjQeL0', 'https://img.youtube.com/vi/YyvSfVjQeL0/hqdefault.jpg', 52),
('Nằm móc đùi sau (Lying Leg Curl). Không nhấc mông khỏi ghế. 3 set x 15 reps.', 'https://www.youtube.com/watch?v=1Tq3QdYUuHs', 'https://img.youtube.com/vi/1Tq3QdYUuHs/hqdefault.jpg', 53),
('Đứng nhún bắp chuối (Standing Calf Raise). Nhún cao gót chân. 3 set x 20 reps.', 'https://www.youtube.com/watch?v=ymkhWBgqwxM', 'https://img.youtube.com/vi/ymkhWBgqwxM/hqdefault.jpg', 54),
('Bài tập mông với tạ (Bulgarian Split Squat). Chân sau đặt trên ghế. 3 set x 12 reps.', 'https://www.youtube.com/watch?v=2C-uNgKwPLE', 'https://img.youtube.com/vi/2C-uNgKwPLE/hqdefault.jpg', 55),
('Deadlift chân thẳng (Romanian Deadlift). Căng cơ đùi sau, lưng thẳng. 3 set x 10 reps.', 'https://www.youtube.com/watch?v=JCXUYuzwNrM', 'https://img.youtube.com/vi/JCXUYuzwNrM/hqdefault.jpg', 56);