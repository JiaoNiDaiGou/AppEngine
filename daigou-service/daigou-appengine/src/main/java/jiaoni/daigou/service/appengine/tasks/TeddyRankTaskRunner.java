package jiaoni.daigou.service.appengine.tasks;

import jiaoni.common.appengine.access.taskqueue.TaskMessage;

import java.util.function.Consumer;

public class TeddyRankTaskRunner implements Consumer<TaskMessage> {
    @Override
    public void accept(TaskMessage taskMessage) {
        
    }
}
