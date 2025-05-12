package tests;

import java.awt.*;
import javax.swing.*;

public class ButtonImageTest extends JFrame {
    JButton button = new JButton();
    Image image = new ImageIcon("resources/buttonTheme.png").getImage();

    ButtonImageTest() {
        setSize(300, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(null);

        button = new JButton("버튼") {
            Image bg = image.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            ImageIcon icon = new ImageIcon(bg);
            Image newBg = icon.getImage();

            @Override
            protected void paintComponent(Graphics g) {
                super.setBorderPainted(false);
                super.setContentAreaFilled(false);
                super.setOpaque(false);

                g.drawImage(newBg, 0, 0, getWidth(), getHeight(), this);
                super.paintComponent(g);
            }
        };
        // button.setBorderPainted(false);
        // button.setContentAreaFilled(false);
        // button.setOpaque(false);

        button.setBounds(50, 50, 50, 50);
        add(button);

        setVisible(true);
    }

    public static void main(String[] args) {
        new ButtonImageTest();
    }
}
