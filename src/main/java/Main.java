import ch.qos.logback.classic.Level;
import org.cloudsimplus.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Titik masuk program.
 *
 * Cara pakai di Windows (dari folder root project):
 *   .\mvnw.cmd compile exec:java "-Dexec.args=OOA"
 *   .\mvnw.cmd compile exec:java "-Dexec.args=CHOA"
 *   .\mvnw.cmd compile exec:java "-Dexec.args=OLB"
 *
 * Opsional, untuk tes cepat:
 *   .\mvnw.cmd compile exec:java "-Dexec.args=OOA 100,200 1"
 *   (argumen ke-2 = daftar jumlah task, argumen ke-3 = jumlah run)
 *
 * Hasil ditulis ke folder hasil/:
 *   hasil_<ALG>.csv       metrik tiap run
 *   konvergensi_<ALG>.csv fitness terbaik per iterasi (OOA dan ChOA saja, OLB tidak punya)
 *   vm_<ALG>.csv          jumlah task dan waktu sibuk tiap VM
 */
public class Main {

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);
        Log.setLevel(Level.WARN);

        if (args.length < 1) {
            System.out.println("Pilih algoritma: OOA, CHOA, atau OLB");
            System.out.println("Contoh: .\\mvnw.cmd compile exec:java \"-Dexec.args=OOA\"");
            return;
        }

        Scheduler scheduler = switch (args[0].toUpperCase()) {
            case "OOA" -> new OoaScheduler(Config.POP_SIZE, Config.MAX_ITER);
            case "CHOA" -> new ChoaScheduler(Config.POP_SIZE, Config.MAX_ITER);
            case "OLB" -> new OlbScheduler();
            default -> throw new IllegalArgumentException(
                    "Algoritma tidak dikenal: " + args[0] + ". Pilih OOA, CHOA, atau OLB");
        };

        int[] taskCounts = args.length >= 2 ? parseList(args[1]) : Config.TASK_COUNTS;
        int runs = args.length >= 3 ? Integer.parseInt(args[2]) : Config.RUNS;

        List<VmSpec> vms = VmSpec.defaultVms();
        long[] dataset = Dataset.loadAll(Config.DATASET_PATH);

        new File(Config.OUTPUT_DIR).mkdirs();
        String alg = scheduler.name();

        System.out.println("==============================================================");
        System.out.printf("  Algoritma : %s%n", alg);
        if (!alg.equals("OLB")) {
            System.out.printf("  Populasi  : %d, Iterasi: %d%n", Config.POP_SIZE, Config.MAX_ITER);
        }
        System.out.printf("  Task      : %s, Run: %d (dataset sekuensial)%n",
                java.util.Arrays.toString(taskCounts), runs);
        System.out.println("==============================================================");

        long t0 = System.currentTimeMillis();

        try (PrintWriter hasil = writer("hasil_" + alg + ".csv");
             PrintWriter konv = alg.equals("OLB") ? null : writer("konvergensi_" + alg + ".csv");
             PrintWriter vmOut = writer("vm_" + alg + ".csv")) {

            hasil.println("algoritma,jumlah_task,run,seed,makespan,total_cost,resource_utilization,"
                    + "degree_of_imbalance,estimasi_makespan,waktu_scheduler_ms,nfe");
            if (konv != null) konv.println("algoritma,jumlah_task,run,iterasi,best_fitness");
            vmOut.println("algoritma,jumlah_task,run,vm_id,tipe,jumlah_task_vm,waktu_sibuk");

            for (int n : taskCounts) {
                long[] tasks = Dataset.firstN(dataset, n);
                for (int r = 1; r <= runs; r++) {
                    long seed = Config.seedFor(n, r);

                    long s0 = System.nanoTime();
                    Scheduler.Result res = scheduler.schedule(tasks, vms, seed);
                    double schedMs = (System.nanoTime() - s0) / 1e6;

                    SimulationRunner.Metrics mt = SimulationRunner.run(tasks, res.assignment(), vms);

                    hasil.printf("%s,%d,%d,%d,%.4f,%.4f,%.4f,%.6f,%.4f,%.2f,%d%n",
                            alg, n, r, seed, mt.makespan(), mt.totalCost(), mt.resourceUtilization(),
                            mt.degreeOfImbalance(), res.estimatedMakespan(), schedMs, res.nfe());

                    if (konv != null && res.convergence() != null) {
                        double[] cv = res.convergence();
                        for (int it = 0; it < cv.length; it++) {
                            konv.printf("%s,%d,%d,%d,%.4f%n", alg, n, r, it, cv[it]);
                        }
                    }
                    for (SimulationRunner.VmStat v : mt.vmStats()) {
                        vmOut.printf("%s,%d,%d,%d,%s,%d,%.4f%n",
                                alg, n, r, v.vmId(), v.type(), v.taskCount(), v.busyTime());
                    }
                    hasil.flush();
                    if (konv != null) konv.flush();
                    vmOut.flush();

                    System.out.printf("[%s] %4d task | run %d | Makespan %10.2f s | Cost $%9.2f | "
                                    + "RU %6.2f%% | DI %.4f | scheduler %.0f ms%n",
                            alg, n, r, mt.makespan(), mt.totalCost(), mt.resourceUtilization(),
                            mt.degreeOfImbalance(), schedMs);
                }
            }
        }

        System.out.printf("%nSelesai dalam %.1f detik. Hasil ada di folder %s/%n",
                (System.currentTimeMillis() - t0) / 1000.0, Config.OUTPUT_DIR);
    }

    private static PrintWriter writer(String fileName) throws IOException {
        return new PrintWriter(new File(Config.OUTPUT_DIR, fileName), StandardCharsets.UTF_8);
    }

    private static int[] parseList(String s) {
        List<Integer> out = new ArrayList<>();
        for (String p : s.split(",")) {
            if (!p.isBlank()) out.add(Integer.parseInt(p.trim()));
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }
}
