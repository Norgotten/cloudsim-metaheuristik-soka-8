import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Osprey Optimization Algorithm (OOA) untuk penjadwalan task.
 *
 * Referensi: Dehghani, M., & Trojovský, P. (2023). Osprey optimization algorithm:
 * A new bio-inspired metaheuristic algorithm for solving engineering optimization
 * problems. Frontiers in Mechanical Engineering, 8, 1126450.
 * https://doi.org/10.3389/fmech.2022.1126450
 *
 * Setiap iterasi, setiap osprey menjalankan dua fase:
 * Fase 1 (eksplorasi, berburu ikan):
 *   pilih satu "ikan" SF secara acak dari osprey yang fitness-nya lebih baik
 *   (ditambah solusi terbaik), lalu
 *   x_baru = x + r * (SF - I * x),  r di [0,1], I acak di {1, 2}
 * Fase 2 (eksploitasi, membawa ikan ke tempat makan):
 *   x_baru = x + (lb + r * (ub - lb)) / t
 * Di kedua fase, posisi baru hanya diterima kalau fitness-nya lebih baik.
 *
 * Populasi awal 100% acak, sesuai paper.
 */
public class OoaScheduler implements Scheduler {

    private final int popSize;
    private final int maxIter;

    public OoaScheduler(int popSize, int maxIter) {
        this.popSize = popSize;
        this.maxIter = maxIter;
    }

    @Override
    public String name() {
        return "OOA";
    }

    @Override
    public Result schedule(long[] taskLengths, List<VmSpec> vms, long seed) {
        Random rng = new Random(seed);
        Fitness fit = new Fitness(taskLengths, vms);
        int dim = fit.dim();
        double lb = fit.lb, ub = fit.ub;

        // ---------- Inisialisasi populasi acak ----------
        double[][] pos = new double[popSize][dim];
        double[] f = new double[popSize];
        double[] best = null;
        double bestFit = Double.POSITIVE_INFINITY;

        for (int i = 0; i < popSize; i++) {
            for (int d = 0; d < dim; d++) {
                pos[i][d] = lb + rng.nextDouble() * (ub - lb);
            }
            f[i] = fit.evaluate(pos[i]);
            if (f[i] < bestFit) {
                bestFit = f[i];
                best = pos[i].clone();
            }
        }

        double[] convergence = new double[maxIter + 1];
        convergence[0] = bestFit;
        double[] cand = new double[dim];
        List<Integer> better = new ArrayList<>(popSize);

        for (int t = 1; t <= maxIter; t++) {
            for (int i = 0; i < popSize; i++) {

                // ---------- Fase 1: identifikasi posisi & berburu ikan ----------
                better.clear();
                for (int k = 0; k < popSize; k++) {
                    if (f[k] < f[i]) better.add(k);
                }
                // himpunan ikan = osprey yang lebih baik + solusi terbaik
                int pick = rng.nextInt(better.size() + 1);
                double[] fish = (pick == better.size()) ? best : pos[better.get(pick)];

                for (int d = 0; d < dim; d++) {
                    double r = rng.nextDouble();
                    int I = 1 + rng.nextInt(2);
                    cand[d] = fit.clamp(pos[i][d] + r * (fish[d] - I * pos[i][d]));
                }
                double fc = fit.evaluate(cand);
                if (fc < f[i]) {
                    System.arraycopy(cand, 0, pos[i], 0, dim);
                    f[i] = fc;
                }

                // ---------- Fase 2: membawa ikan ke tempat makan ----------
                for (int d = 0; d < dim; d++) {
                    double r = rng.nextDouble();
                    cand[d] = fit.clamp(pos[i][d] + (lb + r * (ub - lb)) / t);
                }
                fc = fit.evaluate(cand);
                if (fc < f[i]) {
                    System.arraycopy(cand, 0, pos[i], 0, dim);
                    f[i] = fc;
                }

                if (f[i] < bestFit) {
                    bestFit = f[i];
                    best = pos[i].clone();
                }
            }
            convergence[t] = bestFit;
        }

        return new Result(fit.decode(best), bestFit, convergence, fit.nfe());
    }
}
