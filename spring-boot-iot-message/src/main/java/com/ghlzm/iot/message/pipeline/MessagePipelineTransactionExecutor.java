package com.ghlzm.iot.message.pipeline;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * message pipeline 短事务执行器。
 */
@Component
public class MessagePipelineTransactionExecutor {

    private final TransactionTemplate transactionTemplate;

    public MessagePipelineTransactionExecutor(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void execute(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }
}
