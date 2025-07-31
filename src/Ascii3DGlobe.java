import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class Ascii3DGlobe extends JPanel {
    private final int cols = 150, rows = 100;  // плотность точек
    private final double[][] sphereX, sphereY, sphereZ;
    private double rotationAngleY = 0; // вращение сферы вокруг Oy
    private double rotationAngleX = 0; // вращение суши вокруг Ox (паттерн)
    private double orbitAngle = 0;
    private final double orbitRadius = 280; // радиус орбиты

    public Ascii3DGlobe() {
        sphereX = new double[rows][cols];
        sphereY = new double[rows][cols];
        sphereZ = new double[rows][cols];

        for (int i = 0; i < rows; i++) {
            double theta = Math.PI * (i / (rows - 1.0) - 0.5); // -π/2..π/2
            for (int j = 0; j < cols; j++) {
                double phi = 2 * Math.PI * j / cols;
                sphereX[i][j] = Math.cos(theta) * Math.cos(phi);
                sphereY[i][j] = Math.cos(theta) * Math.sin(phi);
                sphereZ[i][j] = Math.sin(theta);
            }
        }

        new Timer(16, e -> {
            rotationAngleY += 0.02;
            rotationAngleX += 0.015;
            orbitAngle += 0.013;
            repaint();
        }).start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setFont(new Font("Monospaced", Font.PLAIN, 4));  // мелкий шрифт для плотности

        int w = getWidth(), h = getHeight();
        int centerX = w / 2, centerY = h / 2;

        // Рисуем Солнце
        drawAnimatedSunWithRays(g, centerX, centerY, 70);

        // Рисуем орбиту Земли
        g.setColor(Color.DARK_GRAY);
        int orbitDiameter = (int)(orbitRadius * 2);
        g.drawOval(centerX - (int)orbitRadius, centerY - (int)orbitRadius, orbitDiameter, orbitDiameter);

        // Позиция Земли по орбите
        double earthX = centerX + orbitRadius * Math.cos(orbitAngle);
        double earthY = centerY + orbitRadius * Math.sin(orbitAngle);
        int cx = (int) earthX;
        int cy = (int) earthY;

        int size = 140;
        double scale = size / 2.0;

        // **Маскирующий круг, чтобы скрыть часть орбиты "под" Землёй**
        int maskRadius = size / 2 + 3;  // чуть больше радиуса Земли
        g.setColor(Color.BLACK);
        g.fillOval(cx - maskRadius, cy - maskRadius, maskRadius * 2, maskRadius * 2);

        // Рисуем Землю — сферу из ASCII точек
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double x0 = sphereX[i][j];
                double y0 = sphereY[i][j];
                double z0 = sphereZ[i][j];

                // Вращаем сферу вокруг Oy (вертикальная ось)
                double x1 = x0 * Math.cos(rotationAngleY) + z0 * Math.sin(rotationAngleY);
                double z1 = -x0 * Math.sin(rotationAngleY) + z0 * Math.cos(rotationAngleY);
                double y1 = y0;

                if (z1 < 0) continue; // невидимая задняя часть

                int xs = (int)(cx + x1 * scale);
                int ys = (int)(cy + y1 * scale);

                char sym = getEarthTextureCharWithRotation(i, j, rotationAngleX);
                g.setColor(getEarthColor(sym, z1));
                g.drawString(String.valueOf(sym), xs, ys);
            }
        }
    }

    private char getEarthTextureCharWithRotation(int i, int j, double rotX) {
        double lat = Math.PI * (i / (rows - 1.0) - 0.5);
        double lon = 2 * Math.PI * j / cols;

        double y = Math.cos(lat) * Math.sin(lon);
        double z = Math.sin(lat);

        // Вращение паттерна суши вокруг Ox
        double yRot = y * Math.cos(rotX) - z * Math.sin(rotX);
        double zRot = y * Math.sin(rotX) + z * Math.cos(rotX);

        double latRot = Math.asin(zRot);

        double lonDeg = Math.toDegrees(lon);
        if (lonDeg < 0) lonDeg += 360;
        double latDeg = Math.toDegrees(latRot);

        boolean land = isLand(lonDeg, latDeg);
        return land ? '#' : '~';
    }

    private boolean isLand(double lonDeg, double latDeg) {
        lonDeg = (lonDeg + 360) % 360;

        double latRad = Math.toRadians(latDeg);
        double lonRad = Math.toRadians(lonDeg);

        double continentPattern = Math.sin(3 * lonRad) * Math.cos(2 * latRad);
        double noise = 0.3 * Math.sin(10 * lonRad) * Math.sin(5 * latRad);
        double val = continentPattern + noise;

        return val > 0.2;
    }

    private Color getEarthColor(char sym, double z) {
        float brightness = (float) Math.min(1, Math.max(0.3, z));
        if (sym == '#') {
            return new Color(0f, 0.5f * brightness, 0f);
        } else {
            return new Color(0f, 0f, 0.7f * brightness + 0.3f);
        }
    }

    private void drawAnimatedSunWithRays(Graphics g, int x, int y, int radius) {
        Graphics2D g2d = (Graphics2D) g;
        long time = System.currentTimeMillis();
        float phase = (float)(Math.sin(time * 0.004) * 0.5 + 0.5);
        Color coreColor = interpolateColor(new Color(255, 140, 0), new Color(255, 255, 0), phase);

        g2d.setColor(coreColor);
        g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);

        Random rand = new Random(time / 120);
        int rayCount = 25;
        for (int i = 0; i < rayCount; i++) {
            double angle = rand.nextDouble() * 2 * Math.PI;
            double length = 30 + rand.nextDouble() * 50;
            double baseSpread = Math.toRadians(20 + rand.nextDouble() * 10);

            double baseX1 = x + radius * Math.cos(angle - baseSpread);
            double baseY1 = y + radius * Math.sin(angle - baseSpread);
            double baseX2 = x + radius * Math.cos(angle + baseSpread);
            double baseY2 = y + radius * Math.sin(angle + baseSpread);
            double tipX = x + (radius + length) * Math.cos(angle);
            double tipY = y + (radius + length) * Math.sin(angle);

            int[] px = {(int) baseX1, (int) baseX2, (int) tipX};
            int[] py = {(int) baseY1, (int) baseY2, (int) tipY};

            GradientPaint grad = new GradientPaint(
                    (float) ((baseX1 + baseX2) / 2), (float) ((baseY1 + baseY2) / 2), Color.YELLOW,
                    (float) tipX, (float) tipY, Color.RED
            );

            g2d.setPaint(grad);
            g2d.fillPolygon(px, py, 3);
        }
    }

    private Color interpolateColor(Color c1, Color c2, float t) {
        int r = (int)(c1.getRed()   + (c2.getRed()   - c1.getRed())   * t);
        int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t);
        int b = (int)(c1.getBlue()  + (c2.getBlue()  - c1.getBlue())  * t);
        return new Color(r, g, b);
    }

    public static void main(String[] args) {
        JFrame f = new JFrame("ASCII Earth Orbit with Solar Rays");
        Ascii3DGlobe p = new Ascii3DGlobe();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(1200, 900);
        f.setLocationRelativeTo(null);
        f.add(p);
        f.setVisible(true);
    }
}
