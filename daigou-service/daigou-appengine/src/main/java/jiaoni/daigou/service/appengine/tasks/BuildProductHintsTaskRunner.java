package jiaoni.daigou.service.appengine.tasks;

import jiaoni.common.appengine.access.taskqueue.TaskMessage;
import jiaoni.daigou.service.appengine.impls.products.ProductHintsFacade;

import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BuildProductHintsTaskRunner implements Consumer<TaskMessage> {
    private final ProductHintsFacade productHintsFacade;

    @Inject
    public BuildProductHintsTaskRunner(final ProductHintsFacade productHintsFacade) {
        this.productHintsFacade = productHintsFacade;
    }

    @Override
    public void accept(TaskMessage taskMessage) {
        productHintsFacade.buildHints();
    }
}
