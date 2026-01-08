package ShortestRootOptimize;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

public class CityRouteVisualizer extends JFrame {

    static class City {
        String name;
        int x, y;
        City(String n, int x, int y) {
            name = n;
            this.x = x;
            this.y = y;
        }
    }

    static class Edge {
        City to;
        int km;
        Edge(City t, int k) {
            to = t;
            km = k;
        }
    }

    Map<City, List<Edge>> graph = new HashMap<>();
    Map<String, City> cities = new LinkedHashMap<>();

    List<City> fullPath = new ArrayList<>();
    int totalDistance = 0;

    Timer animationTimer;
    int pathIndex = 0;
    double t = 0;
    int carX, carY;

    City srcCity, dstCity;

    JComboBox<String> srcBox, dstBox;
    JTextArea infoArea;
    GraphPanel panel;

    public CityRouteVisualizer() {

        setTitle("SMART CITY ROUTE VISUALIZER");
        setSize(1300, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        createCities();
        createGraph();

        JPanel top = new JPanel();
        top.setBackground(new Color(30, 30, 30));

        JLabel title = new JLabel("SMART CITY ROUTE VISUALIZER (DIJKSTRA ALGORITHM)");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));

        srcBox = new JComboBox<>(cities.keySet().toArray(new String[0]));
        dstBox = new JComboBox<>(cities.keySet().toArray(new String[0]));

        JButton findBtn = new JButton("Find Shortest Path");
        JButton resetBtn = new JButton("Reset Route");

        top.add(title);
        top.add(Box.createHorizontalStrut(25));
        top.add(new JLabel("Source"));
        top.add(srcBox);
        top.add(new JLabel("Destination"));
        top.add(dstBox);
        top.add(findBtn);
        top.add(resetBtn);

        for (Component c : top.getComponents())
            if (c instanceof JLabel) ((JLabel) c).setForeground(Color.WHITE);

        add(top, BorderLayout.NORTH);

        infoArea = new JTextArea(4, 100);
        infoArea.setEditable(false);
        infoArea.setBackground(Color.BLACK);
        infoArea.setForeground(Color.WHITE);
        infoArea.setFont(new Font("Segoe UI", Font.BOLD, 15));
        infoArea.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        panel = new GraphPanel();

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Color.BLACK);
        center.add(infoArea, BorderLayout.NORTH);
        center.add(panel, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);

        findBtn.addActionListener(e -> startRoute());
        resetBtn.addActionListener(e -> resetRoute());

        setVisible(true);
    }

    void startRoute() {
        resetRoute();

        srcCity = cities.get(srcBox.getSelectedItem());
        dstCity = cities.get(dstBox.getSelectedItem());

        dijkstra(srcCity, dstCity);
        if (fullPath.isEmpty()) return;

        carX = srcCity.x;
        carY = srcCity.y;
        pathIndex = 0;
        t = 0;

        animationTimer = new Timer(30, e -> animate());
        animationTimer.start();
    }

    void animate() {
        if (pathIndex >= fullPath.size() - 1) {
            animationTimer.stop();
            infoArea.setText(
                    "Source         : " + srcCity.name + "\n" +
                    "Destination    : " + dstCity.name + "\n" +
                    "Route          : " + formatPath() + "\n" +
                    "Total Distance : " + totalDistance + " km"
            );
            return;
        }

        City a = fullPath.get(pathIndex);
        City b = fullPath.get(pathIndex + 1);

        t += 0.04;
        carX = (int) (a.x + (b.x - a.x) * t);
        carY = (int) (a.y + (b.y - a.y) * t);

        if (t >= 1) {
            t = 0;
            pathIndex++;
        }

        panel.repaint();
    }

    void resetRoute() {
        if (animationTimer != null) animationTimer.stop();
        fullPath.clear();
        totalDistance = 0;
        pathIndex = 0;
        infoArea.setText("");
        panel.repaint();
    }

    void dijkstra(City src, City dst) {

        Map<City, Integer> dist = new HashMap<>();
        Map<City, City> prev = new HashMap<>();

        for (City c : graph.keySet())
            dist.put(c, Integer.MAX_VALUE);

        dist.put(src, 0);

        PriorityQueue<City> pq =
                new PriorityQueue<>(Comparator.comparingInt(dist::get));
        pq.add(src);

        while (!pq.isEmpty()) {
            City u = pq.poll();
            for (Edge e : graph.get(u)) {
                int nd = dist.get(u) + e.km;
                if (nd < dist.get(e.to)) {
                    dist.put(e.to, nd);
                    prev.put(e.to, u);
                    pq.add(e.to);
                }
            }
        }

        totalDistance = dist.get(dst);

        for (City at = dst; at != null; at = prev.get(at))
            fullPath.add(at);

        Collections.reverse(fullPath);
    }

    String formatPath() {
        return String.join(" → ",
                fullPath.stream().map(c -> c.name).toList());
    }

    /* ================= GRAPH PANEL ================= */

    class GraphPanel extends JPanel {

        double zoom = 1.0;
        int panX = 0, panY = 0;
        Point lastMouse;

        GraphPanel() {
            setBackground(Color.BLACK);

            addMouseWheelListener(e -> {
                zoom += e.getPreciseWheelRotation() * -0.05;
                zoom = Math.max(0.4, Math.min(2.5, zoom));
                repaint();
            });

            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    lastMouse = e.getPoint();
                }
            });

            addMouseMotionListener(new MouseAdapter() {
                public void mouseDragged(MouseEvent e) {
                    panX += e.getX() - lastMouse.x;
                    panY += e.getY() - lastMouse.y;
                    lastMouse = e.getPoint();
                    repaint();
                }
            });
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            g2.translate(panX, panY);
            g2.scale(zoom, zoom);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));

            // Draw edges + KM
            for (City c : graph.keySet()) {
                for (Edge e : graph.get(c)) {

                    g2.setColor(Color.GRAY);
                    g2.drawLine(c.x, c.y, e.to.x, e.to.y);

                    int mx = (c.x + e.to.x) / 2;
                    int my = (c.y + e.to.y) / 2;

                    g2.setColor(Color.WHITE);
                    g2.drawString(e.km + " km", mx + 6, my - 6);
                }
            }

            // Highlight path
            g2.setStroke(new BasicStroke(3));
            for (int i = 0; i < fullPath.size() - 1; i++) {
                City a = fullPath.get(i);
                City b = fullPath.get(i + 1);
                g2.setColor(i < pathIndex ? Color.YELLOW : Color.RED);
                g2.drawLine(a.x, a.y, b.x, b.y);
            }

            // Car
            g2.setColor(Color.ORANGE);
            g2.fillOval(carX - 7, carY - 7, 14, 14);

            // Nodes
            for (City c : cities.values()) {
                if (c == srcCity) g2.setColor(Color.GREEN);
                else if (c == dstCity) g2.setColor(Color.CYAN);
                else g2.setColor(Color.LIGHT_GRAY);

                g2.fillOval(c.x - 15, c.y - 15, 30, 30);
                g2.setColor(Color.WHITE);
                g2.drawString(c.name, c.x - 22, c.y + 28);
            }
        }
    }

    /* ================= DATA ================= */

    void addCity(String n, int x, int y) {
        City c = new City(n, x, y);
        cities.put(n, c);
        graph.put(c, new ArrayList<>());
    }

    void connect(String a, String b, int km) {
        City c1 = cities.get(a);
        City c2 = cities.get(b);
        graph.get(c1).add(new Edge(c2, km));
        graph.get(c2).add(new Edge(c1, km));
    }

    void createCities() {
        addCity("Ahmedabad", 140, 520);
        addCity("Gandhinagar", 260, 340);
        addCity("Himatnagar", 380, 300);
        addCity("Vijapur", 500, 340);
        addCity("Visnagar", 620, 340);
        addCity("Mahesana", 740, 550);
        addCity("Kalol", 620, 450);
        addCity("Unjha", 860, 500);
        addCity("Chanshma", 780, 200);
        addCity("Patan", 900, 260);
        addCity("Palanpur", 980, 400);
        addCity("Deesa", 1100, 360);
        addCity("Dhanera", 1200, 250);

        addCity("Khatraj", 260, 550);
        addCity("Kheda", 260, 750);
        addCity("Nadiad", 380, 560);
        addCity("Anand", 500, 560);
        addCity("Vadodara", 620, 750);
    }

    void createGraph() {

        connect("Ahmedabad", "Gandhinagar", 25);
        connect("Kalol", "Ahmedabad", 25);
        connect("Gandhinagar", "Himatnagar", 40);
        connect("Himatnagar", "Vijapur", 25);
        connect("Vijapur", "Visnagar", 30);
        connect("Visnagar", "Mahesana", 35);
        connect("Kalol", "Gandhinagar", 25);
        connect("Mahesana", "Kalol", 40);
        connect("Mahesana", "Unjha", 30);
        connect("Mahesana", "Chanshma", 45);
        connect("Chanshma", "Patan", 20);
        connect("Unjha", "Palanpur", 35);
        connect("Unjha", "Patan", 20);
        connect("Palanpur", "Deesa", 35);
        connect("Patan", "Deesa", 50);
        connect("Deesa", "Dhanera", 20);

        connect("Ahmedabad", "Khatraj", 30);
        connect("Khatraj", "Nadiad", 25);
        connect("Ahmedabad", "Kheda", 35);
        connect("Kheda", "Nadiad", 20);
        connect("Nadiad", "Anand", 15);
        connect("Anand", "Vadodara", 45);
        connect("Ahmedabad", "Vadodara", 100);
        connect("Kheda", "Vadodara", 70);

        connect("Unjha", "Visnagar", 25);
        connect("Visnagar", "Vijapur", 30);
        connect("Vijapur", "Himatnagar", 35);
        connect("Himatnagar", "Gandhinagar", 40);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CityRouteVisualizer::new);
    }
}