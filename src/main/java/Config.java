/**
 * Semua pengaturan eksperimen ada di sini. Ubah di file ini saja.
 */
public final class Config {

    private Config() {}

    // ---------------- Dataset ----------------
    /** Path relatif dari root project. Jalankan perintah mvn dari folder root. */
    public static final String DATASET_PATH = "src/main/resources/dataset/GoCJ_Dataset_1000.csv";

    /** Jumlah task yang diuji. Mode sekuensial: N task = N baris pertama dataset. */
    public static final int[] TASK_COUNTS = {100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};

    /** Berapa kali tiap jumlah task dijalankan. */
    public static final int RUNS = 3;

    // ---------------- Metaheuristik ----------------
    /** Ukuran populasi OOA dan ChOA (dibuat sama supaya adil). */
    public static final int POP_SIZE = Integer.getInteger("pop", 30);

    /** Jumlah iterasi maksimum OOA dan ChOA (dibuat sama supaya adil). */
    public static final int MAX_ITER = Integer.getInteger("iter", 1000);

    /**
     * Inisialisasi hibrida: kalau true, 1 dari 30 agen di populasi awal diisi
     * solusi OLB, sisanya tetap acak. OOA/ChOA lalu memperbaikinya.
     *
     * Alasannya: dengan ratusan sampai 1.000 task, populasi yang 100% acak
     * sulit mencapai solusi yang bagus karena setiap langkah mengubah posisi
     * semua task sekaligus. Ubah ke false untuk versi murni sesuai paper
     * (bisa juga lewat "-DinitOlb=false" tanpa mengubah file ini).
     */
    public static final boolean INIT_WITH_OLB =
            Boolean.parseBoolean(System.getProperty("initOlb", "true"));

    /**
     * Seed acak untuk satu run. Rumusnya tetap, jadi kalau dijalankan ulang
     * hasil OOA/ChOA akan sama persis (bisa direplikasi).
     */
    public static long seedFor(int taskCount, int run) {
        return 1000L * taskCount + run;
    }

    // ---------------- Output ----------------
    public static final String OUTPUT_DIR = "hasil";
}
