import java.util.List;

/**
 * Opportunistic Load Balancing (pembanding).
 * Aturannya sama dengan CloudSim OLB kelompok 8: setiap task diberikan ke VM
 * yang ready time-nya paling kecil, tanpa melihat kecepatan VM.
 * Ready time VM bertambah sebesar panjang task / (jumlah core x MIPS).
 */
public class OlbScheduler implements Scheduler {

    @Override
    public String name() {
        return "OLB";
    }

    @Override
    public Result schedule(long[] taskLengths, List<VmSpec> vms, long seed) {
        int m = vms.size();
        double[] readyTime = new double[m];
        int[] assignment = new int[taskLengths.length];

        for (int i = 0; i < taskLengths.length; i++) {
            int best = 0;
            for (int j = 1; j < m; j++) {
                if (readyTime[j] < readyTime[best]) best = j;
            }
            assignment[i] = best;
            VmSpec vm = vms.get(best);
            readyTime[best] += taskLengths[i] / (vm.pes() * vm.mipsPerPe());
        }

        double est = new Fitness(taskLengths, vms).estimateMakespan(assignment);
        return new Result(assignment, est, null, 0);
    }
}
