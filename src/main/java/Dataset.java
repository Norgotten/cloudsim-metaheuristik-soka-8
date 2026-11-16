import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Membaca dataset GoCJ. Hanya kolom pertama (panjang task dalam MI) yang dipakai.
 */
public final class Dataset {

    private Dataset() {}

    public static long[] loadAll(String path) {
        List<Long> lengths = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                lengths.add(Long.parseLong(line.split(",")[0].trim()));
            }
        } catch (IOException e) {
            throw new RuntimeException("Gagal membaca dataset di " + path
                    + ". Pastikan perintah dijalankan dari folder root project.", e);
        }
        long[] arr = new long[lengths.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = lengths.get(i);
        return arr;
    }

    /** Mode sekuensial: ambil N baris pertama. */
    public static long[] firstN(long[] all, int n) {
        if (n > all.length) {
            throw new IllegalArgumentException("Dataset hanya punya " + all.length + " task, diminta " + n);
        }
        long[] out = new long[n];
        System.arraycopy(all, 0, out, 0, n);
        return out;
    }
}
