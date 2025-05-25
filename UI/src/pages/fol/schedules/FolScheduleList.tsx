import React, { useState } from 'react';
import { 
  useGetFolScheduledTasksQuery, 
  useExecuteScheduledTaskMutation, 
  useCancelScheduledTaskMutation 
} from '../../../api/folApi';

const FolScheduleList: React.FC = () => {
  const { data: tasks, isLoading, error, refetch } = useGetFolScheduledTasksQuery();
  const [executeTask, { isLoading: isExecuting }] = useExecuteScheduledTaskMutation();
  const [cancelTask, { isLoading: isCancelling }] = useCancelScheduledTaskMutation();
  
  const [executingTaskId, setExecutingTaskId] = useState<string | null>(null);
  const [cancellingTaskId, setCancellingTaskId] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleExecuteTask = async (taskId: string) => {
    setExecutingTaskId(taskId);
    setErrorMessage(null);
    try {
      await executeTask(taskId).unwrap();
    } catch (err) {
      console.error('Failed to execute task:', err);
      setErrorMessage('Failed to execute task. Please try again.');
    } finally {
      setExecutingTaskId(null);
    }
  };

  const handleCancelTask = async (taskId: string) => {
    setCancellingTaskId(taskId);
    setErrorMessage(null);
    try {
      await cancelTask(taskId).unwrap();
    } catch (err) {
      console.error('Failed to cancel task:', err);
      setErrorMessage('Failed to cancel task. Please try again.');
    } finally {
      setCancellingTaskId(null);
    }
  };

  const formatDateTime = (dateTimeString?: string) => {
    if (!dateTimeString) return 'N/A';
    const date = new Date(dateTimeString);
    return date.toLocaleString();
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center py-8">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        <p>Failed to load scheduled tasks. Please try refreshing the page.</p>
        <button 
          onClick={() => refetch()} 
          className="mt-2 px-3 py-1 text-xs font-medium text-red-700 bg-red-100 rounded hover:bg-red-200 focus:outline-none"
        >
          Retry
        </button>
      </div>
    );
  }

  return (
    <div>
      {errorMessage && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
          {errorMessage}
        </div>
      )}
    
      {(!tasks || tasks.length === 0) ? (
        <div className="text-center py-8">
          <p className="text-gray-500">No scheduled tasks found.</p>
          <p className="text-sm text-gray-400 mt-2">
            Create file stubs with a cron expression to see them here.
          </p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  File Stub
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Cron Expression
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Last Execution
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Next Execution
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {tasks.map((task) => (
                <tr key={task.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">{task.fileStubName}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900 font-mono">{task.cronExpression}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-500">{formatDateTime(task.lastExecutionTime)}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-500">{formatDateTime(task.nextExecutionTime)}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                      task.status === 'SCHEDULED' ? 'bg-green-100 text-green-800' : 
                      task.status === 'PAUSED' ? 'bg-yellow-100 text-yellow-800' : 
                      'bg-red-100 text-red-800'
                    }`}>
                      {task.status}
                    </span>
                    {task.errorMessage && (
                      <div className="text-xs text-red-500 mt-1">{task.errorMessage}</div>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    <button
                      onClick={() => handleExecuteTask(task.id)}
                      disabled={isExecuting && executingTaskId === task.id}
                      className="text-primary-600 hover:text-primary-900 mr-4 disabled:opacity-50"
                    >
                      {isExecuting && executingTaskId === task.id ? (
                        <span className="flex items-center">
                          <span className="animate-spin -ml-1 mr-2 h-4 w-4 border-b-2 border-primary-600 rounded-full"></span>
                          Executing...
                        </span>
                      ) : (
                        'Execute Now'
                      )}
                    </button>
                    <button
                      onClick={() => handleCancelTask(task.id)}
                      disabled={isCancelling && cancellingTaskId === task.id}
                      className="text-red-600 hover:text-red-900 disabled:opacity-50"
                    >
                      {isCancelling && cancellingTaskId === task.id ? (
                        <span className="flex items-center">
                          <span className="animate-spin -ml-1 mr-2 h-4 w-4 border-b-2 border-red-600 rounded-full"></span>
                          Cancelling...
                        </span>
                      ) : (
                        'Cancel'
                      )}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default FolScheduleList; 