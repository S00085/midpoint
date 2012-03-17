/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.task.impl;

import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author Radovan Semancik
 * 
 */
public class SingleRunner extends TaskRunner {

	private static final transient Trace LOGGER = TraceManager.getTrace(SingleRunner.class);

	public SingleRunner(TaskHandler handler, Task task, TaskManagerImpl taskManager) {
		super(handler,task,taskManager);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		logRunStart();

		try {
			
			RepositoryCache.enter();
			
			// This is NOT the result of the run itself. That can be found in
			// the RunResult
			// this is a result of the runner, used to record "overhead" things
			// like recording the
			// run status
			OperationResult runnerRunOpResult = new OperationResult(SingleRunner.class.getName() + ".run");
			
	
			try {
				task.recordRunStart(runnerRunOpResult);
			} catch (ObjectNotFoundException ex) {
				LOGGER.error("Unable to record run start: {}", ex.getMessage(), ex);
			} catch (SchemaException ex) {
				LOGGER.error("Unable to record run start: {}", ex.getMessage(), ex);
			} // there are otherwise quite safe to ignore

			
			TaskRunResult runResult = null;
			
			try {

				// here we execute the whole handler stack
				// FIXME do a better run result reporting! (currently only the last runResult gets reported)
				while (handler != null && task.canRun()) {
					runResult = handler.run(task);
					task.finishHandler(runnerRunOpResult);
					handler = taskManager.getHandler(task.getHandlerUri());
				}

			} catch (Exception ex) {
				LOGGER.error("Task handler threw unexpected exception: {}: {}",new Object[] { ex.getClass().getName(),ex.getMessage(),ex});
				runResult = new TaskRunResult();
				OperationResult dummyResult = new OperationResult(SingleRunner.class.getName() + ".error");
				dummyResult.recordFatalError("Task handler threw unexpected exception: "+ex.getMessage(),ex);
				runResult.setOperationResult(dummyResult);
				runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			}

			// record this run (this will also save the OpResult)
			// TODO: figure out how to do better error handling
			if (runResult == null) {
				// Obviously error in task handler
				LOGGER.error("Unable to record run finish: task returned null result");
				runResult = new TaskRunResult();
				OperationResult dummyResult = new OperationResult(SingleRunner.class.getName() + ".error");
				dummyResult.recordFatalError("Unable to record run finish: task returned null result");
				runResult.setOperationResult(dummyResult);
				runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			} else {
				try {
					task.recordRunFinish(runResult, runnerRunOpResult);
					// Run finished. So let's close the task
					// However, if the task was shut down externally, we will not close it;
					// probably it will have to be run again when resumed or when midPoint will be restarted
					if (task.canRun())
						task.close(runnerRunOpResult);
				} catch (ObjectNotFoundException ex) {
					LOGGER.error("Unable to record run finish: {}", ex.getMessage(), ex);
					// The task object in repo is gone. Therefore this task should not run any more.
					// Therefore commit sepukku
					taskManager.removeRunner(this);		// originally here was shutdownAndRemoveRunner - however, we cannot wait for completion of the thread we are executed in :)
					RepositoryCache.exit();
					return;
				} catch (SchemaException ex) {
					LOGGER.error("Unable to record run finish and close the task: {}", ex.getMessage(), ex);
				} // there are otherwise quite safe to ignore
			}
						
			// Call back task manager to clean up things
			taskManager.finishRunnableTask(this, task, runnerRunOpResult);

			logRunFinish();

		} catch (Throwable t) {
			// This is supposed to run in a thread, so this kind of heavy artillery is needed. If throwable won't be
			// caught here, nobody will catch it and it won't even get logged.
			LOGGER.error("SingleRunner got critical exception, the task state cannot be saved: {}: {}",new Object[] { t.getClass().getName(),t.getMessage(), t});
		} finally {
			RepositoryCache.exit();
		}
	}

}
