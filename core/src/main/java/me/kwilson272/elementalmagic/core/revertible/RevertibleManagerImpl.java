package me.kwilson272.elementalmagic.core.revertible;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import me.kwilson272.elementalmagic.api.revertible.Revertible;
import me.kwilson272.elementalmagic.api.revertible.RevertibleManager;

public class RevertibleManagerImpl implements RevertibleManager {
    
    private final Set<Revertible> instances;
    private final Queue<Revertible> revertQueue;
    
    public RevertibleManagerImpl() {
        instances = new HashSet<>();
        // Ascending based on earliest revert time
        revertQueue = new PriorityQueue<>((r1, r2) -> 
                (int) (r1.getRevertTimeMillis() - r2.getRevertTimeMillis()));
    }

	@Override
	public void enable() {
	}

	@Override
	public void disable(boolean shutDown) {
        revertAll();
	}

	@Override
	public void register(Revertible revertible) {
        // Treat duration <= 0 as a manual/shutdown reversions 
        if (revertible.getDurationMillis() > 0) {
            revertQueue.add(revertible);
        }
        instances.add(revertible);
	}

	@Override
	public void revert(Revertible revertible) {
        if (!instances.remove(revertible)) {
            return;
        }

        revertible.handleRevertTasks();
        if (revertible.getDurationMillis() > 0) {
            revertQueue.remove(revertible);
        }
	}

	@Override
	public void revertAll() {
        instances.forEach(Revertible::handleRevertTasks);
        instances.clear();
        revertQueue.clear();
	}

    @Override
    public void revertExpired() {
        long time = System.currentTimeMillis();
        while (!revertQueue.isEmpty()) {
            if (revertQueue.peek().getRevertTimeMillis() > time) {
                break;
            }

            Revertible revertible = revertQueue.poll();
            revertible.handleRevertTasks();
            instances.remove(revertible);
        }
    }
}
