import React from 'react';
import FolScheduleList from './FolScheduleList';
import { useGetSchedulerInfoQuery } from '../../../api/folApi';

const FolScheduler: React.FC = () => {
  const { data: schedulerInfo, isLoading } = useGetSchedulerInfoQuery();
  
  return (
    <div>
      {schedulerInfo && !isLoading && (
        <div className="mb-6">
          <div className="flex flex-wrap gap-4">
            <div className="bg-white p-4 rounded-lg shadow flex-1 min-w-[200px]">
              <h3 className="text-sm font-medium text-gray-500">Scheduler Status</h3>
              <p className="mt-2 text-xl font-semibold">
                {schedulerInfo.isRunning ? (
                  <span className="text-green-600">Running</span>
                ) : (
                  <span className="text-red-600">Stopped</span>
                )}
              </p>
            </div>
            
            <div className="bg-white p-4 rounded-lg shadow flex-1 min-w-[200px]">
              <h3 className="text-sm font-medium text-gray-500">Active Tasks</h3>
              <p className="mt-2 text-xl font-semibold">{schedulerInfo.activeTaskCount}</p>
            </div>
            
            <div className="bg-white p-4 rounded-lg shadow flex-1 min-w-[200px]">
              <h3 className="text-sm font-medium text-gray-500">Total Executions</h3>
              <p className="mt-2 text-xl font-semibold">{schedulerInfo.totalExecutions}</p>
            </div>
            
            <div className="bg-white p-4 rounded-lg shadow flex-1 min-w-[200px]">
              <h3 className="text-sm font-medium text-gray-500">Server Time</h3>
              <p className="mt-2 text-xl font-semibold text-gray-700">
                {new Date(schedulerInfo.serverTime).toLocaleString()}
              </p>
            </div>
          </div>
        </div>
      )}
      
      <div className="bg-white p-6 rounded-lg shadow">
        <h2 className="text-lg font-medium text-gray-900 mb-4">Scheduled Tasks</h2>
        <FolScheduleList />
      </div>
      
      <div className="mt-8 bg-white p-6 rounded-lg shadow">
        <h2 className="text-lg font-medium text-gray-900 mb-4">Cron Expression Help</h2>
        <div className="prose max-w-none">
          <p>
            Cron expressions are used to configure when tasks should be executed. A cron expression consists of six fields:
          </p>
          <pre className="bg-gray-100 p-3 rounded text-sm overflow-x-auto">
            <code>second minute hour day-of-month month day-of-week</code>
          </pre>
          <h3 className="text-base font-medium mt-4">Examples:</h3>
          <ul className="mt-2">
            <li><code>0 0/15 * * * *</code> - Every 15 minutes</li>
            <li><code>0 0 12 * * *</code> - Every day at 12pm</li>
            <li><code>0 0 0 * * MON</code> - Every Monday at midnight</li>
            <li><code>0 0 0 1 * *</code> - First day of every month at midnight</li>
          </ul>
          <p className="mt-4">
            For more information, see the <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/support/CronExpression.html" target="_blank" rel="noopener noreferrer" className="text-primary-600 hover:text-primary-700">Spring documentation</a>.
          </p>
        </div>
      </div>
    </div>
  );
};

export default FolScheduler;
