// Импортируем библиотеку Swing для создания окон, кнопок, панелей и текстовых меток
import javax.swing.*;
// Импортируем библиотеку AWT для работы с цветами (Color), шрифтами (Font) и размещением элементов (Layout)
import java.awt.*;

/**
 * Главный класс приложения.
 * Наследуется от JFrame — это стандартное окно операционной системы (с рамкой, заголовком и крестиком закрытия).
 */
public class SnakeGame extends JFrame {

    // Поле для хранения ссылки на нашу игровую панель, где происходит вся логика и отрисовка
    private final GamePanel gamePanel;

    // Кнопка, которая меняет свое назначение: Старт -> Пауза -> Продолжить -> Рестарт
    private final JButton controlButton;

    // Текстовая метка для отображения количества съеденных яблок
    private final JLabel scoreLabel;

    // Выпадающий список для выбора уровня скорости змейки
    private final JComboBox<String> levelBox;

    // Константы цветов в формате RGB (Красный, Зеленый, Синий от 0 до 255) для текста на кнопке
    private final Color RED_TEXT = new Color(210, 40, 40);   // Насыщенный красный для "Старт" и "Рестарт"
    private final Color GREEN_TEXT = new Color(30, 150, 50); // Приятный зеленый для "Пауза" и "Продолжить"

    /**
     * Конструктор главного окна.
     * Срабатывает один раз при запуске программы и собирает весь интерфейс.
     */
    public SnakeGame() {
        // Устанавливаем заголовок в верхней рамке окна
        setTitle("Змейка на Java");

        // Указываем, что при нажатии на крестик программа должна полностью завершить работу
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Запрещаем пользователю растягивать окно мышкой, чтобы не сломать сетку игрового поля
        setResizable(false);

        // Устанавливаем менеджер компоновки BorderLayout.
        // Он делит окно на 5 зон: Север (верх), Юг (низ), Запад (лево), Восток (право) и Центр.
        setLayout(new BorderLayout());

        // ================= СОЗДАНИЕ ВЕРХНЕЙ ПАНЕЛИ =================
        // Создаем панель, которая будет лежать в верхней части окна (BorderLayout.NORTH)
        JPanel topPanel = new JPanel(new BorderLayout());
        // Красим фон верхней панели в темно-серый цвет
        topPanel.setBackground(new Color(30, 30, 30));
        // Добавляем невидимые внутренние отступы (сверху 10px, слева 15px, снизу 10px, справа 15px),
        // чтобы элементы не прилипали вплотную к краям окна
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // ----------------- Левая часть: Счетчик -----------------
        scoreLabel = new JLabel("Яблок: 0");
        scoreLabel.setForeground(Color.WHITE); // Белый цвет текста
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 16)); // Жирный шрифт Arial, размер 16

        // ----------------- Правая часть: Скорость и Кнопка -----------------
        // Создаем вспомогательную панель с выравниванием элементов по правому краю (FlowLayout.RIGHT)
        // и горизонтальным отступом 12 пикселей между элементами
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        // Делаем эту панель прозрачной, чтобы через нее просвечивал темно-серый фон topPanel
        rightControls.setOpaque(false);

        JLabel speedLabel = new JLabel("Скорость:");
        speedLabel.setForeground(Color.LIGHT_GRAY);
        speedLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        // Массив строк, который отображается пользователю в выпадающем списке
        String[] levels = {
                "Ур. 1",
                "Ур. 2",
                "Ур. 3",
                "Ур. 4",
                "Ур. 5",
                "Ур. 6",
                "Ур. 7",
                "Ур. 8",
                "Ур. 9",
                "Ур. 10"
        };

        // Создаем сам выпадающий список на основе массива строк
        levelBox = new JComboBox<>(levels);
        // Выбираем первый элемент по умолчанию (индексация в Java начинается с 0)
        levelBox.setSelectedIndex(0);
        levelBox.setFont(new Font("Arial", Font.PLAIN, 13));
        // ВАЖНО: Запрещаем списку перехватывать фокус клавиатуры, чтобы нажатие стрелок "Вверх/Вниз"
        // управляло змейкой, а не переключало уровни скорости во время игры!
        levelBox.setFocusable(false);

        // ----------------- Настройка универсальной кнопки -----------------
        controlButton = new JButton("Старт");
        // Жестко фиксируем размер кнопки (ширина 135px, высота 32px), чтобы она не дергалась при смене текста
        controlButton.setPreferredSize(new Dimension(135, 32));
        controlButton.setFont(new Font("Arial", Font.BOLD, 15));
        controlButton.setBackground(Color.WHITE); // Белый фон кнопки
        controlButton.setForeground(RED_TEXT);    // Начальный цвет текста — красный
        // setOpaque(true) заставляет Swing принудительно закрасить фон кнопки нашим белым цветом,
        // игнорируя стандартную системную тему Windows или macOS
        controlButton.setOpaque(true);
        // Рисуем тонкую серую рамку толщиной 1 пиксель вокруг кнопки
        controlButton.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        // Убираем пунктирную рамку вокруг текста при нажатии
        controlButton.setFocusPainted(false);
        // ВАЖНО: Отключаем фокус у кнопки! Если не сделать этого, то после клика мышкой по кнопке
        // нажатие "Пробела" будет повторно нажимать саму кнопку штатными средствами Windows,
        // а не обрабатываться нашим игровым кодом.
        controlButton.setFocusable(false);

        // Складываем подпись, список скоростей и кнопку в правую вспомогательную панель
        rightControls.add(speedLabel);
        rightControls.add(levelBox);
        rightControls.add(controlButton);

        // Размещаем счетчик слева, а блок управления справа на верхней панели
        topPanel.add(scoreLabel, BorderLayout.WEST);
        topPanel.add(rightControls, BorderLayout.EAST);

        // ================= СОЗДАНИЕ ИГРОВОГО ПОЛЯ =================
        // Создаем экземпляр игровой панели и передаем ей ссылку 'this' (на само окно SnakeGame),
        // чтобы игровая панель могла обновлять текст счета и кнопки
        gamePanel = new GamePanel(this);
        // Синхронизируем начальную скорость игровой панели с выбранным пунктом в списке
        gamePanel.setSpeedLevel(levelBox.getSelectedIndex() + 1);

        // ================= ОБРАБОТКА СОБЫТИЙ (СЛУШАТЕЛИ) =================
        // Слушатель нажатия мышкой на кнопку: вызывает метод переключения состояния игры
        controlButton.addActionListener(e -> gamePanel.toggleStartPauseRestart());

        // Слушатель выбора нового уровня в выпадающем списке
        levelBox.addActionListener(e -> {
            // Получаем номер выбранного уровня (индекс + 1, чтобы получить числа от 1 до 10)
            int selectedLevel = levelBox.getSelectedIndex() + 1;
            // Передаем новый уровень в игровую панель для мгновенного изменения скорости
            gamePanel.setSpeedLevel(selectedLevel);
        });

        // Добавляем собранную верхнюю панель в верхнюю часть окна, а игровое поле — в центр
        add(topPanel, BorderLayout.NORTH);
        add(gamePanel, BorderLayout.CENTER);

        // Метод pack() автоматически подгоняет размер окна под точные размеры вложенных панелей
        pack();
        // Размещаем окно строго по центру экрана монитора
        setLocationRelativeTo(null);
        // Делаем окно видимым для пользователя
        setVisible(true);
    }

    /**
     * Публичный метод, который вызывает GamePanel каждый раз, когда змейка съедает яблоко.
     */
    public void updateScore(int score) {
        scoreLabel.setText("Яблок: " + score);
    }

    /**
     * Публичный метод для смены надписи и цвета текста на кнопке управления.
     * @param text Новый текст кнопки ("Старт", "Пауза", "Продолжить", "Рестарт")
     * @param isWarningColor true — покрасить текст в красный, false — покрасить в зеленый
     */
    public void updateButtonState(String text, boolean isWarningColor) {
        controlButton.setText(text);
        controlButton.setForeground(isWarningColor ? RED_TEXT : GREEN_TEXT);
    }

    /**
     * Точка входа в программу. С этого метода Java начинает выполнение кода.
     */
    public static void main(String[] args) {
        // SwingUtilities.invokeLater запускает графический интерфейс в специальном потоке (EDT — Event Dispatch Thread).
        // Это золотое правило Java Swing, гарантирующее, что интерфейс не зависнет и не возникнет ошибок отрисовки.
        SwingUtilities.invokeLater(SnakeGame::new);
    }
}
