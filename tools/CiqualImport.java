import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convertit la table Ciqual 2025 de l'ANSES (fichiers XML publiés sur Zenodo, licence ouverte Etalab 2.0 / CC-BY 4.0)
 * en un petit fichier TSV lu par l'appli : un aliment par ligne, valeurs pour 100 g.
 *
 * <pre>
 * java tools/CiqualImport.java &lt;sortie.tsv&gt;                  télécharge depuis Zenodo
 * java tools/CiqualImport.java &lt;sortie.tsv&gt; &lt;dossier&gt;        lit les XML d'un dossier (essais)
 * </pre>
 *
 * Lancé par la tâche Gradle « ciqualImport » avant chaque compilation. En cas d'échec, la compilation échoue.
 */
public class CiqualImport {

    /** Enregistrement « concept » Zenodo de la table Ciqual (toujours la dernière version). */
    static final String RECORD = "17550132";
    static final String API = "https://zenodo.org/api/records/" + RECORD + "/versions/latest";
    /** Noms des fichiers de la version 2025, utilisés si l'API ne répond pas. */
    static final String[] FALLBACK = {"alim_2025_11_03.xml", "alim_grp_2025_11_03.xml", "compo_2025_11_03.xml"};

    static final int KJ = 327, KCAL = 328, PROTEIN = 25000, CARBS = 31000, FAT = 40000, FIBER = 34100, SUGARS = 32000;
    static final int[] KEEP = {KJ, KCAL, PROTEIN, CARBS, FAT, FIBER, SUGARS};

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage : java CiqualImport.java <sortie.tsv> [dossier des XML]");
            System.exit(2);
        }
        File out = new File(args[0]);
        String alim, grp, compo;
        if (args.length >= 2) {
            File dir = new File(args[1]);
            alim = decode(Files.readAllBytes(find(dir, "alim_", "alim_grp_").toPath()));
            grp = decode(Files.readAllBytes(find(dir, "alim_grp_", null).toPath()));
            compo = decode(Files.readAllBytes(find(dir, "compo_", null).toPath()));
        } else {
            Map<String, String> links = zenodoLinks();
            alim = decode(download(pick(links, "alim_", "alim_grp_")));
            grp = decode(download(pick(links, "alim_grp_", null)));
            compo = decode(download(pick(links, "compo_", null)));
        }
        List<Row> rows = convert(alim, grp, compo);
        check(rows);
        write(rows, out);
        System.out.println("Ciqual : " + rows.size() + " aliments écrits dans " + out);
    }

    // ---------------------------------------------------------------- téléchargement

    static File find(File dir, String prefix, String exclude) throws IOException {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                String n = f.getName();
                if (n.startsWith(prefix) && n.endsWith(".xml") && (exclude == null || !n.startsWith(exclude))) return f;
            }
        }
        throw new IOException("Fichier " + prefix + "*.xml introuvable dans " + dir);
    }

    /** Liens de téléchargement de la dernière version, par nom de fichier. */
    static Map<String, String> zenodoLinks() {
        Map<String, String> links = new LinkedHashMap<>();
        String api = System.getProperty("ciqual.api", API);
        try {
            String json = new String(download(api), StandardCharsets.UTF_8);
            // Liens de l'API (…/api/records/<id>/files/<nom>[/content]) ou de la page (…/records/<id>/files/<nom>?download=1).
            Matcher m = Pattern.compile("(https?://[^\"]+?/api/records/\\d+/files/)([^\"/?]+)(/content)?\"").matcher(json);
            while (m.find()) {
                String key = java.net.URLDecoder.decode(m.group(2), "UTF-8");
                if (!links.containsKey(key)) links.put(key, m.group(1) + m.group(2) + "/content");
            }
            if (links.isEmpty()) {
                Matcher d = Pattern.compile("(https?://[^\"]+?/records/\\d+/files/)([^\"/?]+)").matcher(json);
                while (d.find()) {
                    String key = java.net.URLDecoder.decode(d.group(2), "UTF-8");
                    if (!links.containsKey(key)) links.put(key, d.group(1) + d.group(2) + "?download=1");
                }
            }
        } catch (Exception e) {
            System.out.println("API Zenodo indisponible (" + e.getMessage() + "), liens directs utilisés.");
        }
        if (links.isEmpty()) {
            String base = System.getProperty("ciqual.files", "https://zenodo.org/records/" + RECORD + "/files/");
            for (String f : FALLBACK) links.put(f, base + f + "?download=1");
        }
        return links;
    }

    static String pick(Map<String, String> links, String prefix, String exclude) throws IOException {
        for (Map.Entry<String, String> e : links.entrySet()) {
            String n = e.getKey();
            if (n.startsWith(prefix) && n.endsWith(".xml") && (exclude == null || !n.startsWith(exclude))) return e.getValue();
        }
        throw new IOException("Fichier " + prefix + "*.xml absent de l'enregistrement Zenodo " + RECORD);
    }

    static byte[] download(String url) throws IOException {
        IOException last = null;
        for (int attempt = 1; attempt <= 4; attempt++) {
            HttpURLConnection con = null;
            try {
                con = open(url, 0);
                int status = con.getResponseCode();
                if (status == 404 || status == 410) throw new java.io.FileNotFoundException("HTTP " + status + " pour " + url);
                if (status != 200) throw new IOException("HTTP " + status + " pour " + url);
                try (InputStream in = con.getInputStream()) {
                    ByteArrayOutputStream buf = new ByteArrayOutputStream(1 << 20);
                    byte[] chunk = new byte[1 << 16];
                    int r;
                    while ((r = in.read(chunk)) > 0) buf.write(chunk, 0, r);
                    return buf.toByteArray();
                }
            } catch (java.io.FileNotFoundException e) {
                throw e;
            } catch (IOException e) {
                last = e;
                System.out.println("Téléchargement " + attempt + "/4 échoué : " + e.getMessage());
                try {
                    Thread.sleep(3000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } finally {
                if (con != null) con.disconnect();
            }
        }
        throw last != null ? last : new IOException("Téléchargement impossible : " + url);
    }

    /** Ouvre la connexion en suivant les redirections (y compris entre http et https). */
    static HttpURLConnection open(String url, int depth) throws IOException {
        HttpURLConnection con = (HttpURLConnection) java.net.URI.create(url).toURL().openConnection();
        con.setConnectTimeout(30000);
        con.setReadTimeout(120000);
        con.setInstanceFollowRedirects(false);
        con.setRequestProperty("User-Agent", "Fonte-build/1.0 (https://github.com/sulliv4nx/FONTES)");
        con.setRequestProperty("Accept", "*/*");
        int status = con.getResponseCode();
        if (status >= 300 && status < 400 && depth < 5) {
            String next = con.getHeaderField("Location");
            con.disconnect();
            if (next == null) throw new IOException("Redirection sans adresse : " + url);
            return open(java.net.URI.create(url).resolve(next).toString(), depth + 1);
        }
        return con;
    }

    // ---------------------------------------------------------------- lecture des XML

    /** Décode selon l'encodage déclaré (windows-1252 dans les anciennes tables, UTF-8 sinon). */
    static String decode(byte[] raw) {
        String head = new String(raw, 0, Math.min(raw.length, 200), StandardCharsets.ISO_8859_1);
        Matcher m = Pattern.compile("encoding=[\"']([A-Za-z0-9_-]+)[\"']").matcher(head);
        Charset cs = StandardCharsets.UTF_8;
        if (m.find()) {
            try {
                cs = Charset.forName(m.group(1));
            } catch (Exception ignored) {
                cs = StandardCharsets.UTF_8;
            }
        }
        String s = new String(raw, cs);
        if (s.length() > 0 && s.charAt(0) == '﻿') s = s.substring(1);
        return s;
    }

    /** Blocs &lt;TAG&gt;…&lt;/TAG&gt;, lus sans analyseur XML (les tables contiennent parfois des « &lt; » non échappés). */
    static List<String> blocks(String xml, String tag) {
        List<String> out = new ArrayList<>();
        String open = "<" + tag + ">", close = "</" + tag + ">";
        int i = 0;
        while (true) {
            int a = xml.indexOf(open, i);
            if (a < 0) break;
            int b = xml.indexOf(close, a + open.length());
            if (b < 0) break;
            out.add(xml.substring(a + open.length(), b));
            i = b + close.length();
        }
        return out;
    }

    /** Valeur d'un champ dans un bloc, nettoyée (espaces, entités XML), ou null. */
    static String field(String block, String tag) {
        String open = "<" + tag + ">";
        int a = block.indexOf(open);
        if (a < 0) return null;
        int b = block.indexOf("</" + tag + ">", a);
        if (b < 0) return null;
        String v = block.substring(a + open.length(), b);
        v = v.replace("<![CDATA[", "").replace("]]>", "");
        v = v.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&");
        v = v.replace(' ', ' ').replaceAll("\\s+", " ").trim();
        return v.isEmpty() ? null : v;
    }

    /** Teneur : nombre (virgule ou point), « traces » et « &lt; x » valent 0, « - » ou vide = inconnue (NaN). */
    static double value(String v) {
        if (v == null) return Double.NaN;
        String s = v.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty() || s.equals("-") || s.equals("missing")) return Double.NaN;
        if (s.startsWith("traces") || s.startsWith("<")) return 0;
        s = s.replace(',', '.').replace(" ", "");
        try {
            double d = Double.parseDouble(s);
            return d < 0 ? Double.NaN : d;
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    static int code(String v) {
        if (v == null) return -1;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    static final class Row {
        int code;
        String name;
        String grp;
        String group;
        String grpName;
        double[] v = new double[KEEP.length];

        Row() {
            for (int i = 0; i < v.length; i++) v[i] = Double.NaN;
        }

        double get(int constCode) {
            for (int i = 0; i < KEEP.length; i++) if (KEEP[i] == constCode) return v[i];
            return Double.NaN;
        }
    }

    static List<Row> convert(String alimXml, String grpXml, String compoXml) {
        Map<String, String> grpNames = new HashMap<>();
        Map<String, String> ssgrpNames = new HashMap<>();
        for (String b : blocks(grpXml, "ALIM_GRP")) {
            String g = field(b, "alim_grp_code");
            if (g == null) continue;
            String gn = field(b, "alim_grp_nom_fr");
            if (gn != null) grpNames.put(g, gn);
            String sg = field(b, "alim_ssgrp_code");
            String sgn = field(b, "alim_ssgrp_nom_fr");
            if (sg != null && sgn != null && !sgn.equals("-")) ssgrpNames.put(g + "/" + sg, sgn);
        }
        Map<Integer, Row> rows = new LinkedHashMap<>();
        for (String b : blocks(alimXml, "ALIM")) {
            int c = code(field(b, "alim_code"));
            String name = field(b, "alim_nom_fr");
            if (c < 0 || name == null) continue;
            Row r = new Row();
            r.code = c;
            r.name = name;
            String g = field(b, "alim_grp_code");
            String sg = field(b, "alim_ssgrp_code");
            r.grp = g == null ? "" : g;
            String group = ssgrpNames.get(r.grp + "/" + sg);
            if (group == null) group = grpNames.get(r.grp);
            r.group = group == null ? "" : group;
            String top = grpNames.get(r.grp);
            r.grpName = top == null ? "" : top;
            rows.put(c, r);
        }
        int[] wanted = KEEP;
        for (String b : blocks(compoXml, "COMPO")) {
            int cc = code(field(b, "const_code"));
            int idx = -1;
            for (int i = 0; i < wanted.length; i++) if (wanted[i] == cc) idx = i;
            if (idx < 0) continue;
            Row r = rows.get(code(field(b, "alim_code")));
            if (r == null) continue;
            r.v[idx] = value(field(b, "teneur"));
        }
        List<Row> out = new ArrayList<>();
        for (Row r : rows.values()) {
            String g = (r.group + " " + r.grpName).toLowerCase(Locale.ROOT);
            // Les petits pots et laits infantiles n'ont pas leur place dans un carnet de sportif.
            if (g.contains("infantil")) continue;
            double kcal = r.get(KCAL);
            if (Double.isNaN(kcal) && !Double.isNaN(r.get(KJ))) kcal = r.get(KJ) / 4.184;
            double p = r.get(PROTEIN), c = r.get(CARBS), f = r.get(FAT), fib = r.get(FIBER);
            if (Double.isNaN(kcal) && !(Double.isNaN(p) && Double.isNaN(c) && Double.isNaN(f))) {
                kcal = 4 * z(p) + 4 * z(c) + 9 * z(f) + 2 * z(fib);
            }
            if (Double.isNaN(kcal)) continue;
            r.v[index(KCAL)] = kcal;
            out.add(r);
        }
        return out;
    }

    static int index(int constCode) {
        for (int i = 0; i < KEEP.length; i++) if (KEEP[i] == constCode) return i;
        throw new IllegalArgumentException("constituant " + constCode);
    }

    static double z(double v) {
        return Double.isNaN(v) ? 0 : v;
    }

    static void check(List<Row> rows) {
        if (rows.size() < 2000) {
            throw new IllegalStateException("Table Ciqual incomplète : " + rows.size() + " aliments lus (au moins 2 000 attendus).");
        }
        int withProtein = 0;
        for (Row r : rows) if (!Double.isNaN(r.get(PROTEIN))) withProtein++;
        if (withProtein < rows.size() * 0.8) {
            throw new IllegalStateException("Table Ciqual douteuse : protéines connues pour " + withProtein + " aliments sur " + rows.size() + ".");
        }
    }

    static String num(double v) {
        if (Double.isNaN(v)) return "";
        double r = Math.round(v * 10) / 10.0;
        if (r == Math.rint(r)) return String.valueOf((long) r);
        return String.valueOf(r);
    }

    static String clean(String s) {
        return s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    static void write(List<Row> rows, File out) throws IOException {
        File parent = out.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new IOException("Dossier impossible à créer : " + parent);
        File tmp = new File(out.getPath() + ".tmp");
        try (Writer w = new OutputStreamWriter(Files.newOutputStream(tmp.toPath()), StandardCharsets.UTF_8)) {
            w.write("# Table Ciqual 2025, ANSES (https://ciqual.anses.fr), licence ouverte Etalab 2.0 / CC-BY 4.0. Converti le "
                    + LocalDate.now() + ". Valeurs pour 100 g.\n");
            w.write("# code\tnom\tgroupe_code\tgroupe\tkcal\tproteines\tglucides\tlipides\tfibres\tsucres\n");
            for (Row r : rows) {
                w.write(r.code + "\t" + clean(r.name) + "\t" + clean(r.grp) + "\t" + clean(r.group) + "\t" + num(r.get(KCAL)) + "\t"
                        + num(r.get(PROTEIN)) + "\t" + num(r.get(CARBS)) + "\t" + num(r.get(FAT)) + "\t" + num(r.get(FIBER)) + "\t"
                        + num(r.get(SUGARS)) + "\n");
            }
        }
        Files.move(tmp.toPath(), out.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}
