package skyscrapper;

import java.io.*;
import java.util.*;


public class Leaderboard {

    public static class Entry {
        public final String name;
        public final int score;
        public final double seconds;

        public Entry(String name, int score, double seconds) {
            this.name = name;
            this.score = score;
            this.seconds = seconds;
        }
    }

    private static final int MAX_ENTRIES = 3;

    private final List<Entry> entries = new ArrayList<>();
    private final File file = new File(System.getProperty("user.home"), "skyscrapper_top3.properties");

    public Leaderboard() {
        load();
    }

    public void add(String name, int score, double seconds) {
        entries.add(new Entry(name, score, seconds));
        sortAndTrim();
    }

    public List<Entry> top3() {
        return new ArrayList<>(entries);
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("count", String.valueOf(entries.size()));

        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            p.setProperty("e" + i + ".name", e.name);
            p.setProperty("e" + i + ".score", String.valueOf(e.score));
            p.setProperty("e" + i + ".seconds", String.valueOf(e.seconds));
        }

        try (OutputStream out = new FileOutputStream(file)) {
            p.store(out, "Skyscrapper Leaderboard (Top 3)");
        } catch (IOException ignored) {
        }
    }

    public void load() {
        entries.clear();
        if (!file.exists()) return;

        Properties p = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            p.load(in);

            int count = parseInt(p.getProperty("count"), 0);

            for (int i = 0; i < count; i++) {
                String name = p.getProperty("e" + i + ".name", "Player");
                int score = parseInt(p.getProperty("e" + i + ".score"), 0);
                double seconds = parseDouble(p.getProperty("e" + i + ".seconds"), 0);

                entries.add(new Entry(name, score, seconds));
            }

            sortAndTrim();
        } catch (IOException ignored) {
        }
    }

    private void sortAndTrim() {
        entries.sort((a, b) -> {
            if (b.score != a.score) return Integer.compare(b.score, a.score);
            return Double.compare(a.seconds, b.seconds);
        });

        while (entries.size() > MAX_ENTRIES) {
            entries.remove(entries.size() - 1);
        }
    }

    private int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (Exception e) { return fallback; }
    }

    private double parseDouble(String s, double fallback) {
        try { return Double.parseDouble(s); } catch (Exception e) { return fallback; }
    }
}



