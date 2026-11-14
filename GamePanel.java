import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;

/**
 * Игровая панель.
 * Наследуется от JPanel (холст для рисования) и реализует интерфейс Runnable,
 * чтобы крутить игровой цикл в отдельном потоке с высокой точностью (System.nanoTime()).
 */
public class GamePanel extends JPanel implements Runnable {

    // ================= НАСТРОЙКИ ИГРОВОГО ПОЛЯ =================
    private static final int SCREEN_WIDTH = 600;  // Ширина игрового поля в пикселях
    private static final int SCREEN_HEIGHT = 600; // Высота игрового поля в пикселях
    private static final int UNIT_SIZE = 25;      // Размер одной клетки (и одного сегмента змейки) в пикселях

    // Максимально возможное количество клеток на поле (600 * 600) / (25 * 25) = 576 клеток.
    private static final int GAME_UNITS = (SCREEN_WIDTH * SCREEN_HEIGHT) / (UNIT_SIZE * UNIT_SIZE);

    // ================= СОСТОЯНИЕ ЗМЕЙКИ И ЕДЫ =================
    // x[0] и y[0] — координаты ГОЛОВЫ змейки.
    private final int[] x = new int[GAME_UNITS];
    private final int[] y = new int[GAME_UNITS];

    private int bodyParts = 3;   // Текущая длина змейки
    private int applesEaten = 0; // Счетчик съеденных яблок
    private int appleX;          // Координата X текущего яблока
    private int appleY;          // Координата Y текущего яблока

    // Направление движения: 'U' (вверх), 'D' (вниз), 'L' (влево), 'R' (вправо)
    private char direction = 'R';

    // Защита от двойного нажатия клавиш за один шаг
    private boolean canChangeDirection = true;

    // Состояния игры (volatile гарантирует, что фоновый поток и окно видят актуальное состояние без задержек)
    private enum GameState {
        READY,
        RUNNING,
        PAUSED,
        GAME_OVER
    }
    private volatile GameState state = GameState.READY;

    // ================= СИСТЕМНЫЕ ОБЪЕКТЫ И ПОТОК =================
    private Thread gameThread;            // Отдельный поток для точного игрового цикла
    private volatile long stepIntervalNs; // Интервал одного шага змейки в наносекундах
    private final Random random;          // Генератор случайных чисел
    private final SnakeGame parentFrame;  // Ссылка на главное окно

    /**
     * Конструктор игровой панели.
     */
    public GamePanel(SnakeGame parentFrame) {
        this.parentFrame = parentFrame;
        this.random = new Random();

        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(new Color(20, 20, 20));
        setFocusable(true);
        addKeyListener(new MyKeyAdapter());

        // Начальная скорость: 500 миллисекунд = 500_000_000 наносекунд
        this.stepIntervalNs = 500_000_000L;

        // Подготавливаем начальные координаты змейки и первое яблоко
        initGameData();

        // Запускаем отдельный фоновый поток игрового цикла
        gameThread = new Thread(this, "SnakeGameLoopThread");
        gameThread.start();
    }

    /**
     * Пересчитывает уровень сложности (от 1 до 10) в интервал шага в наносекундах.
     * 1 миллисекунда = 1 000 000 наносекунд.
     */
    public void setSpeedLevel(int level) {
        long delayMs = 500L - (level - 1) * 50L;
        this.stepIntervalNs = delayMs * 1_000_000L;
        requestFocusInWindow();
    }

    /**
     * Сбрасывает все игровые параметры к начальным значениям.
     */
    private void initGameData() {
        bodyParts = 3;
        applesEaten = 0;
        direction = 'R';
        parentFrame.updateScore(applesEaten);

        int startX = (SCREEN_WIDTH / 2 / UNIT_SIZE) * UNIT_SIZE;
        int startY = (SCREEN_HEIGHT / 2 / UNIT_SIZE) * UNIT_SIZE;

        for (int i = 0; i < bodyParts; i++) {
            x[i] = startX - (i * UNIT_SIZE);
            y[i] = startY;
        }

        spawnApple();
    }

    /**
     * Главный метод управления состоянием игры (кнопка и ПРОБЕЛ).
     */
    public void toggleStartPauseRestart() {
        switch (state) {
            case READY:
                state = GameState.RUNNING;
                parentFrame.updateButtonState("Пауза", false);
                break;

            case RUNNING:
                state = GameState.PAUSED;
                parentFrame.updateButtonState("Продолжить", false);
                repaint();
                break;

            case PAUSED:
                state = GameState.RUNNING;
                parentFrame.updateButtonState("Пауза", false);
                break;

            case GAME_OVER:
                initGameData();
                state = GameState.RUNNING;
                parentFrame.updateButtonState("Пауза", false);
                break;
        }
        requestFocusInWindow();
    }
    /**
     * Генерирует случайные координаты для нового яблока так, чтобы оно не попало на тело змейки.
     */
    private void spawnApple() {
        boolean onSnake;
        do {
            onSnake = false;
            appleX = random.nextInt(SCREEN_WIDTH / UNIT_SIZE) * UNIT_SIZE;
            appleY = random.nextInt(SCREEN_HEIGHT / UNIT_SIZE) * UNIT_SIZE;

            for (int i = 0; i < bodyParts; i++) {
                if (x[i] == appleX && y[i] == appleY) {
                    onSnake = true;
                    break;
                }
            }
        } while (onSnake);
    }

    /**
     * Системный метод Swing для перерисовки холста.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    /**
     * Вся логика отрисовки графики: сетка, яблоко, змейка, таблички и синхронизация кадра.
     */
    private void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Рисуем фоновую сетку
        g2d.setColor(new Color(35, 35, 35));
        for (int i = 0; i < SCREEN_WIDTH / UNIT_SIZE; i++) {
            g2d.drawLine(i * UNIT_SIZE, 0, i * UNIT_SIZE, SCREEN_HEIGHT);
            g2d.drawLine(0, i * UNIT_SIZE, SCREEN_WIDTH, i * UNIT_SIZE);
        }

        // 2. Рисуем яблоко
        g2d.setColor(new Color(220, 50, 50));
        g2d.fillOval(appleX + 2, appleY + 2, UNIT_SIZE - 4, UNIT_SIZE - 4);

        // 3. Рисуем змейку (индекс 0 — голова, остальные — тело)
        for (int i = 0; i < bodyParts; i++) {
            if (i == 0) {
                g2d.setColor(new Color(50, 220, 100));
            } else {
                g2d.setColor(new Color(35, 160, 75));
            }
            g2d.fillRoundRect(x[i] + 1, y[i] + 1, UNIT_SIZE - 2, UNIT_SIZE - 2, 8, 8);
        }

        // 4. Информационные плашки поверх экрана
        if (state == GameState.READY) {
            drawOverlayMessage(g2d, "Нажмите «Старт» или ПРОБЕЛ", Color.WHITE);
        } else if (state == GameState.PAUSED) {
            drawOverlayMessage(g2d, "ПАУЗА (Пробел — продолжить)", Color.YELLOW);
        } else if (state == GameState.GAME_OVER) {
            drawOverlayMessage(g2d, "ИГРА ОКОНЧЕНА! Счет: " + applesEaten, new Color(255, 80, 80));
        }

        // 5. Принудительная синхронизация видеодрайвера (убирает задержки буфера кадров на macOS и Linux)
        Toolkit.getDefaultToolkit().sync();
    }

    /**
     * Вспомогательный метод для отрисовки текста строго по центру экрана.
     */
    private void drawOverlayMessage(Graphics2D g2d, String message, Color color) {
        g2d.setColor(new Color(0, 0, 0, 160));
        g2d.fillRect(0, SCREEN_HEIGHT / 2 - 40, SCREEN_WIDTH, 80);

        g2d.setColor(color);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        FontMetrics metrics = getFontMetrics(g2d.getFont());
        int textX = (SCREEN_WIDTH - metrics.stringWidth(message)) / 2;
        int textY = (SCREEN_HEIGHT / 2) + (metrics.getAscent() / 2) - 4;
        g2d.drawString(message, textX, textY);
    }

    /**
     * Метод отвечает за один шаг (тик) передвижения всей змейки по игровому полю.
     * Работает в четыре этапа: подтягивает хвост, сдвигает голову, проверяет границы (телепортацию) и разблокирует кнопки.
     */
    private void move() {
        // Заводим переменную для нулевого индекса (голова змейки в массивах x и y всегда лежит под номером 0)
        int head = 0;

        // ================= ШАГ 1: ПЕРЕДВИЖЕНИЕ ТЕЛА ЗМЕЙКИ =================
        // Перемещаем каждый сегмент тела на место предыдущего, двигаясь с самого кончика хвоста к голове.
        // Важно идти именно с конца (i = bodyParts) к началу (i > 0):
        // если пойти с головы, мы перезапишем координаты первого сегмента до того, как второй успеет их скопировать.
        // Например: 3-й сегмент встает на место 2-го, 2-й на место 1-го, а 1-й на место головы (индекс head).
        for (int i = bodyParts; i > 0; i--) {
            x[i] = x[i - 1]; // Копируем координату X впереди идущего сегмента
            y[i] = y[i - 1]; // Копируем координату Y впереди идущего сегмента
        }

        // ================= ШАГ 2: СДВИГ ГОЛОВЫ В НОВОМ НАПРАВЛЕНИИ =================
        // Теперь, когда 1-й сегмент уже скопировал старую позицию головы, саму голову (индекс head)
        // можно сдвинуть на одну клетку (UNIT_SIZE = 25 пикселей) в выбранном направлении.
        // В компьютерной графике точка (0, 0) находится в ЛЕВОМ ВЕРХНЕМ углу окна:
        // ось X растет вправо, а ось Y растет вниз.
        switch (direction) {
            case 'U': // Up (Вверх)
                y[head] -= UNIT_SIZE; // Двигаемся вверх к нулю -> вычитаем размер одной клетки из Y
                break;
            case 'D': // Down (Вниз)
                y[head] += UNIT_SIZE; // Двигаемся вниз от нуля -> прибавляем размер одной клетки к Y
                break;
            case 'L': // Left (Влево)
                x[head] -= UNIT_SIZE; // Двигаемся влево к нулю -> вычитаем размер одной клетки из X
                break;
            case 'R': // Right (Вправо)
                x[head] += UNIT_SIZE; // Двигаемся вправо от нуля -> прибавляем размер одной клетки к X
                break;
        }

        // ================= ШАГ 3: ПРОХОЖДЕНИЕ СКВОЗЬ СТЕНЫ (ТЕЛЕПОРТАЦИЯ) =================
        // Проверяем координаты головы (x[head] и y[head]), не вышла ли она за пределы холста (от 0 до 575 пикселей).
        // Почему 575, а не 600? Потому что поле шириной 600px делится на 24 клетки по 25px (индексы от 0 до 23).
        // Координата левого края самой последней 24-й клетки равна: 600 - 25 = 575.

        // Проверка горизонтальных границ (левая и правая стены)
        if (x[head] < 0) {
            // Голова ушла левее нуля (координата стала -25) -> переносим ее в самую правую клетку (575)
            x[head] = SCREEN_WIDTH - UNIT_SIZE;
        } else if (x[head] >= SCREEN_WIDTH) {
            // Голова достигла или перешла правую границу (600) -> переносим ее в самую левую клетку (0)
            x[head] = 0;
        }

        // Проверка вертикальных границ (верхняя и нижняя стены)
        if (y[head] < 0) {
            // Голова ушла выше потолка (координата стала -25) -> переносим ее в самую нижнюю клетку (575)
            y[head] = SCREEN_HEIGHT - UNIT_SIZE;
        } else if (y[head] >= SCREEN_HEIGHT) {
            // Голова достигла или перешла пол (600) -> переносим ее в самую верхнюю клетку (0)
            y[head] = 0;
        }

        // ================= ШАГ 4: РАЗБЛОКИРОВКА УПРАВЛЕНИЯ =================
        // Физический шаг змейки полностью завершен и координаты обновлены.
        // Снова поднимаем флаг canChangeDirection, разрешая игроку нажать стрелку для следующего поворота.
        canChangeDirection = true;
    }

    /**
     * Проверяет, не совпали ли координаты головы змейки (x[head], y[head]) с координатами яблока (appleX, appleY).
     */
    private void checkApple() {
        int head = 0;

        // Так как и змейка, и яблоко всегда выровнены по сетке с шагом 25 пикселей,
        // нам не нужно проверять пересечение прямоугольников — достаточно точного равенства их координат.
        if (x[head] == appleX && y[head] == appleY) {
            bodyParts++;   // Увеличиваем счетчик длины змейки на 1. Новый сегмент появится на следующем шаге цикла
            applesEaten++; // Увеличиваем счетчик съеденных яблок на 1

            // ВАЖНО ДЛЯ МНОГОПОТОЧНОСТИ: так как этот метод вызывается из нашего фонового потока (gameThread),
            // менять текст на компонентах Swing (JLabel) напрямую из него небезопасно.
            // SwingUtilities.invokeLater ставит задачу обновления счета в специальный поток интерфейса (EDT).
            SwingUtilities.invokeLater(() -> parentFrame.updateScore(applesEaten));

            spawnApple();  // Генерируем новое яблоко в случайной свободной клетке
        }
    }

    /**
     * Проверяет, не врезалась ли голова змейки в собственный хвост.
     * Вызывается на каждом шаге игрового цикла сразу после движения головы.
     */
    private void checkCollisions() {
        int head = 0;

        // Пробегаем циклом по всем сегментам тела, начиная с первого сегмента после головы (i = 1)
        // и заканчивая кончиком хвоста (i = bodyParts).
        // Важно: начинаем именно с i = 1, потому что i = 0 (head) — это сама голова, и она всегда равна сама себе!
        for (int i = bodyParts; i > 0; i--) {
            // Если координаты головы (x[head], y[head]) полностью совпали с координатами любого куска тела (x[i], y[i])
            if (x[head] == x[i] && y[head] == y[i]) {
                triggerGameOver(); // Запускаем процедуру окончания игры (смена состояния и текста кнопки на "Рестарт")
                return;            // Досрочно выходим из метода, так как столкновение уже зафиксировано
            }
        }
    }

    /**
     * Завершает игру при столкновении.
     */
    private void triggerGameOver() {
        state = GameState.GAME_OVER;
        SwingUtilities.invokeLater(() -> parentFrame.updateButtonState("Рестарт", true));
    }

    /**
     * Главный игровой цикл (Game Loop), работающий в отдельном потоке с высокой точностью System.nanoTime().
     */
    @Override
    public void run() {
        long lastTime = System.nanoTime();
        long accumulatedTime = 0;

        while (gameThread != null) {
            long currentTime = System.nanoTime();
            long elapsedTime = currentTime - lastTime;
            lastTime = currentTime;

            if (state == GameState.RUNNING) {
                accumulatedTime += elapsedTime;

                // Когда накопилось достаточно наносекунд для одного шага змейки
                if (accumulatedTime >= stepIntervalNs) {
                    move();
                    checkApple();
                    checkCollisions();
                    repaint();

                    // Вычитаем интервал шага, сохраняя остаток времени для компенсации любых микрозадержек
                    accumulatedTime -= stepIntervalNs;
                }
            } else {
                // На паузе или в меню не копим время, чтобы после старта змейка не сделала резкий рывок
                accumulatedTime = 0;
            }

            // Короткая пауза в 1 миллисекунду, чтобы фоновый поток не нагружал процессор на 100%
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Внутренний класс для обработки нажатий клавиш на клавиатуре.
     */
    private class MyKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int keyCode = e.getKeyCode();

            if (keyCode == KeyEvent.VK_SPACE) {
                toggleStartPauseRestart();
                return;
            }

            if (state != GameState.RUNNING || !canChangeDirection) {
                return;
            }

            switch (keyCode) {
                case KeyEvent.VK_LEFT:
                    if (direction != 'R') {
                        direction = 'L';
                        canChangeDirection = false;
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    if (direction != 'L') {
                        direction = 'R';
                        canChangeDirection = false;
                    }
                    break;
                case KeyEvent.VK_UP:
                    if (direction != 'D') {
                        direction = 'U';
                        canChangeDirection = false;
                    }
                    break;
                case KeyEvent.VK_DOWN:
                    if (direction != 'U') {
                        direction = 'D';
                        canChangeDirection = false;
                    }
                    break;
            }
        }
    }
}
