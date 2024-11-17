package slimeknights.mantle.data.listener;

import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.profiler.Profiler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * This interface is similar to {@link net.minecraft.resource.SynchronousResourceReloader}, except it runs during {@link net.minecraft.resource.SinglePreparationResourceReloader}'s prepare phase.
 * This is used mainly as models load during the prepare phase, so it ensures they are loaded soon enough.
 * <p>
 * TODO 1.19: is there any reason to keep this alongside {@link IEarlySafeManagerReloadListener}?
 */
public interface IEarlyReloadListener extends ResourceReloader {
    @Override
    default CompletableFuture<Void> reload(Synchronizer stage, ResourceManager resourceManager, Profiler preparationsProfiler, Profiler reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.runAsync(() -> {
            this.onResourceManagerReload(resourceManager);
        }, backgroundExecutor).thenCompose(stage::whenPrepared);
    }

    /**
     * @param resourceManager the resource manager being reloaded
     */
    void onResourceManagerReload(ResourceManager resourceManager);
}
