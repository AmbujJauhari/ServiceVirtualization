import React, { useState } from 'react';
import { 
  useGetStubsQuery,
  useUpdateActiveStatusMutation,
  useDeleteStubMutation,
  Stub
} from '../../api/stubApi';
import { Link } from 'react-router-dom';

export const StubList: React.FC = () => {
  const { data: stubs, isLoading, isError } = useGetStubsQuery();
  const [updateActiveStatus] = useUpdateActiveStatusMutation();
  const [deleteStub] = useDeleteStubMutation();
  const [error, setError] = useState('');
  const [togglingIds, setTogglingIds] = useState<Set<string>>(new Set());
  const [deletingIds, setDeletingIds] = useState<Set<string>>(new Set());
  
  const handleToggleActive = async (id: string, currentActive: boolean) => {
    try {
      setTogglingIds(prev => new Set(prev).add(id));
      await updateActiveStatus({ id, active: !currentActive });
    } catch (err) {
      console.error('Failed to update active status:', err);
      setError('Failed to update active status. Please try again.');
    } finally {
      setTogglingIds(prev => {
        const newSet = new Set(prev);
        newSet.delete(id);
        return newSet;
      });
    }
  };

  const handleDelete = async (id: string) => {
    if (window.confirm('Are you sure you want to delete this stub?')) {
      try {
        setDeletingIds(prev => new Set(prev).add(id));
        await deleteStub(id);
      } catch (err) {
        console.error('Failed to delete stub:', err);
        setError('Failed to delete stub. Please try again.');
      } finally {
        setDeletingIds(prev => {
          const newSet = new Set(prev);
          newSet.delete(id);
          return newSet;
        });
      }
    }
  };

  if (isLoading) return <div className="flex justify-center p-4">Loading...</div>;
  if (isError) return <div className="flex justify-center p-4 text-red-500">Error loading stubs</div>;

  return (
    <div className="bg-white shadow-md rounded-lg overflow-hidden">
      <div className="flex justify-between items-center p-4 border-b">
        <h2 className="text-xl font-semibold text-gray-700">Stubs</h2>
        <Link 
          to="/stubs/new" 
          className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition-colors"
        >
          New Stub
        </Link>
      </div>

      {error && (
        <div className="p-3 m-4 bg-red-100 text-red-700 rounded">
          {error}
          <button 
            onClick={() => setError('')} 
            className="ml-2 text-red-700 hover:text-red-900"
          >
            ✕
          </button>
        </div>
      )}

      {stubs && stubs.length > 0 ? (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Name</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Protocol</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tags</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {stubs.map((stub: Stub) => (
                <tr key={stub.id}>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">{stub.name}</div>
                    <div className="text-sm text-gray-500">{stub.description}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-blue-100 text-blue-800">
                      {stub.protocol}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex flex-wrap gap-1">
                      {stub.tags && stub.tags.map(tag => (
                        <span 
                          key={tag} 
                          className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-gray-100 text-gray-800"
                        >
                          {tag}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <button
                      onClick={() => handleToggleActive(stub.id, stub.status === 'ACTIVE')}
                      disabled={togglingIds.has(stub.id)}
                      className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                        stub.status === 'ACTIVE' 
                          ? 'bg-green-100 text-green-800 hover:bg-green-200' 
                          : 'bg-gray-100 text-gray-800 hover:bg-gray-200'
                      } ${togglingIds.has(stub.id) ? 'opacity-50 cursor-not-allowed' : ''}`}
                    >
                      {togglingIds.has(stub.id) 
                        ? 'Updating...' 
                        : (stub.status)}
                    </button>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    <div className="flex space-x-2">
                      <Link 
                        to={`/stubs/${stub.id}`}
                        className="text-indigo-600 hover:text-indigo-900"
                      >
                        View
                      </Link>
                      <Link 
                        to={`/stubs/${stub.id}/edit`}
                        className="text-blue-600 hover:text-blue-900"
                      >
                        Edit
                      </Link>
                      <button 
                        onClick={() => handleDelete(stub.id)}
                        disabled={deletingIds.has(stub.id)}
                        className={`text-red-600 hover:text-red-900 ${
                          deletingIds.has(stub.id) ? 'opacity-50 cursor-not-allowed' : ''
                        }`}
                      >
                        {deletingIds.has(stub.id) ? 'Deleting...' : 'Delete'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="p-4 text-center text-gray-500">
          No stubs found. Convert recordings to stubs or create new stubs manually.
        </div>
      )}
    </div>
  );
}; 