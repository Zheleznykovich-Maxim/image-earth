import javax.swing.*;
import java.awt.*;

public class Ascii3DGlobe extends JPanel {
    private final int cols = 80, rows = 40;
    private final double[][] sphereX, sphereY, sphereZ;
    private double angle = 0;

    public Ascii3DGlobe() {
        sphereX = new double[rows][cols];
        sphereY = new double[rows][cols];
        sphereZ = new double[rows][cols];
        // Инициализация сферической сетки
        for (int i = 0; i < rows; i++) {
            double theta = Math.PI * (i / (rows - 1.0) - 0.5);
            for (int j = 0; j < cols; j++) {
                double phi = 2 * Math.PI * j / cols;
                sphereX[i][j] = Math.cos(theta) * Math.cos(phi);
                sphereY[i][j] = Math.cos(theta) * Math.sin(phi);
                sphereZ[i][j] = Math.sin(theta);
            }
        }
        // Таймер для плавного вращения (~60 FPS)
        new Timer(16, e -> { angle += 0.02; repaint(); }).start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        int w = getWidth(), h = getHeight();
        int cx = w / 2, cy = h / 2;
        int size = Math.min(w, h) - 20;
        double scale = size / 2.0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // координаты после поворота вокруг оси Y
                double x0 = sphereX[i][j];
                double z0 = sphereZ[i][j];
                double x1 = x0 * Math.cos(angle) - z0 * Math.sin(angle);
                double z1 = x0 * Math.sin(angle) + z0 * Math.cos(angle);
                double y1 = sphereY[i][j];
                if (z1 < 0) continue; // рисуем только ближнюю сторону

                int xs = (int)(cx + x1 * scale);
                int ys = (int)(cy + y1 * scale);
                char sym = pickChar(y1, z1);
                g.setColor(pickColor(sym));
                g.drawString(String.valueOf(sym), xs, ys);
            }
        }
    }

    private char pickChar(double y, double z) {
        if (z > 0.5) return '#';      // суша
        if (y > 0.0) return '.';      // облака или свет
        return '~';                   // океан
    }

    private Color pickColor(char c) {
        return switch (c) {
            case '#' -> Color.GREEN;
            case '.' -> Color.LIGHT_GRAY;
            case '~' -> new Color(0, 170, 255);
            default -> Color.WHITE;
        };
    }

    public static void main(String[] args) {
        JFrame f = new JFrame("3D ASCII Globe");
        Ascii3DGlobe p = new Ascii3DGlobe();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(600, 600);
        f.setLocationRelativeTo(null);
        f.add(p);
        f.setVisible(true);
    }
}
