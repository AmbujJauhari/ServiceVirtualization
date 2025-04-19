import React, { useState } from 'react';
import { useGetSystemHealthWithPolling } from '../../api/healthApi';
import StatusDetailModal from './StatusDetailModal';

const StatusIndicator: React.FC = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const { data: healthData, error, isLoading } = useGetSystemHealthWithPolling();

  // Determine indicator color based on health status
  const getStatusColor = () => {
    if (isLoading) return 'bg-gray-400'; // Loading state
    if (error) return 'bg-red-500';      // Error state
    
    switch (healthData?.overall) {
      case 'UP':
        return 'bg-green-500';
      case 'DEGRADED':
        return 'bg-yellow-500';
      case 'DOWN':
        return 'bg-red-500';
      default:
        return 'bg-gray-400';
    }
  };

  // Get status text for tooltip
  const getStatusText = () => {
    if (isLoading) return 'Loading...';
    if (error) return 'Error loading health status';
    
    switch (healthData?.overall) {
      case 'UP':
        return 'All systems operational';
      case 'DEGRADED':
        return 'Some services degraded';
      case 'DOWN':
        return 'System outage detected';
      default:
        return 'Unknown status';
    }
  };

  return (
    <div className="relative">
      <button 
        className="flex items-center focus:outline-none group"
        onClick={() => setIsModalOpen(true)}
        title={getStatusText()}
      >
        <div className="flex items-center">
          <div className={`h-3 w-3 rounded-full ${getStatusColor()} mr-2`}></div>
          <span className="text-sm text-gray-600 dark:text-gray-400 group-hover:text-gray-900 dark:group-hover:text-gray-200">
            Status
          </span>
        </div>
      </button>

      {isModalOpen && (
        <StatusDetailModal 
          healthData={healthData} 
          isOpen={isModalOpen}
          isLoading={isLoading}
          error={error}
          onClose={() => setIsModalOpen(false)} 
        />
      )}
    </div>
  );
};

export default StatusIndicator; 