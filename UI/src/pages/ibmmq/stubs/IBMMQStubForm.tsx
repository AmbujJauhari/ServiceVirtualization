import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { 
  useGetIBMMQStubQuery, 
  useCreateIBMMQStubMutation, 
  useUpdateIBMMQStubMutation,
  IBMMQStub,
  MessageHeader
} from '../../../api/ibmMqApi';

/**
 * Form for creating and editing IBM MQ stubs
 */
const IBMMQStubForm: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const isEditMode = Boolean(id);
  const navigate = useNavigate();

  // Fetch existing stub for edit mode
  const { data: existingStub, isLoading: isLoadingStub } = useGetIBMMQStubQuery(id ?? '', {
    skip: !isEditMode
  });

  // Mutations
  const [createStub, { isLoading: isCreating }] = useCreateIBMMQStubMutation();
  const [updateStub, { isLoading: isUpdating }] = useUpdateIBMMQStubMutation();

  // Form state
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [queueManager, setQueueManager] = useState('');
  const [queueName, setQueueName] = useState('');
  const [selector, setSelector] = useState('');
  const [responseType, setResponseType] = useState('text');
  const [responseContent, setResponseContent] = useState('');
  const [latency, setLatency] = useState(0);
  const [headers, setHeaders] = useState<MessageHeader[]>([]);
  const [status, setStatus] = useState(true);

  // New header form fields
  const [newHeaderName, setNewHeaderName] = useState('');
  const [newHeaderValue, setNewHeaderValue] = useState('');
  const [newHeaderType, setNewHeaderType] = useState('string');

  // Populate form with existing data in edit mode
  useEffect(() => {
    if (existingStub) {
      setName(existingStub.name || '');
      setDescription(existingStub.description || '');
      setQueueManager(existingStub.queueManager || '');
      setQueueName(existingStub.queueName || '');
      setSelector(existingStub.selector || '');
      setResponseType(existingStub.responseType || 'text');
      setResponseContent(existingStub.responseContent || '');
      setLatency(existingStub.latency || 0);
      setHeaders(existingStub.headers || []);
      setStatus(existingStub.status ?? true);
    }
  }, [existingStub]);

  const handleAddHeader = () => {
    if (!newHeaderName.trim()) return;

    const newHeader: MessageHeader = {
      name: newHeaderName,
      value: newHeaderValue,
      type: newHeaderType
    };

    setHeaders([...headers, newHeader]);
    setNewHeaderName('');
    setNewHeaderValue('');
    setNewHeaderType('string');
  };

  const handleRemoveHeader = (headerName: string) => {
    setHeaders(headers.filter(header => header.name !== headerName));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const stubData: Partial<IBMMQStub> = {
      name,
      description,
      queueManager,
      queueName,
      selector,
      responseType,
      responseContent,
      latency,
      headers,
      status
    };

    try {
      if (isEditMode && id) {
        await updateStub({
          id,
          ...stubData
        });
      } else {
        await createStub(stubData);
      }
      navigate('/ibmmq');
    } catch (error) {
      console.error('Error saving IBM MQ stub:', error);
      alert('Failed to save IBM MQ stub. Please try again.');
    }
  };

  if (isEditMode && isLoadingStub) {
    return <div className="text-center py-4">Loading...</div>;
  }

  return (
    <div className="container mx-auto p-4">
      <div className="bg-white shadow-md rounded-lg p-6">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">
          {isEditMode ? 'Edit IBM MQ Stub' : 'Create IBM MQ Stub'}
        </h1>

        <form onSubmit={handleSubmit}>
          {/* Basic Information */}
          <div className="mb-6">
            <h2 className="text-lg font-semibold text-gray-700 mb-4">Basic Information</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="name">
                  Name*
                </label>
                <input
                  id="name"
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
                  required
                />
              </div>
              <div>
                <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="description">
                  Description
                </label>
                <input
                  id="description"
                  type="text"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
                />
              </div>
            </div>
          </div>

          {/* Queue Configuration */}
          <div className="mb-6">
            <h2 className="text-lg font-semibold text-gray-700 mb-4">Queue Configuration</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="queueManager">
                  Queue Manager*
                </label>
                <input
                  id="queueManager"
                  type="text"
                  value={queueManager}
                  onChange={(e) => setQueueManager(e.target.value)}
                  className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
                  required
                />
              </div>
              <div>
                <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="queueName">
                  Queue Name*
                </label>
                <input
                  id="queueName"
                  type="text"
                  value={queueName}
                  onChange={(e) => setQueueName(e.target.value)}
                  className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
                  required
                />
              </div>
            </div>
            <div className="mt-4">
              <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="selector">
                Message Selector
              </label>
              <input
                id="selector"
                type="text"
                value={selector}
                onChange={(e) => setSelector(e.target.value)}
                placeholder="e.g. JMSCorrelationID='12345'"
                className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
              />
              <p className="text-sm text-gray-500 mt-1">
                Selector expression to filter which messages this stub responds to.
              </p>
            </div>
          </div>

          {/* Response Configuration */}
          <div className="mb-6">
            <h2 className="text-lg font-semibold text-gray-700 mb-4">Response Configuration</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="responseType">
                  Response Type
                </label>
                <select
                  id="responseType"
                  value={responseType}
                  onChange={(e) => setResponseType(e.target.value)}
                  className="shadow border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
                >
                  <option value="text">Text</option>
                  <option value="json">JSON</option>
                  <option value="xml">XML</option>
                  <option value="bytes">Bytes</option>
                </select>
              </div>
              <div>
                <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="latency">
                  Response Latency (ms)
                </label>
                <input
                  id="latency"
                  type="number"
                  min="0"
                  value={latency}
                  onChange={(e) => setLatency(parseInt(e.target.value) || 0)}
                  className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
                />
                <p className="text-sm text-gray-500 mt-1">
                  Add artificial delay to simulate processing time.
                </p>
              </div>
            </div>

            <div className="mt-4">
              <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="responseContent">
                Response Content
              </label>
              <textarea
                id="responseContent"
                value={responseContent}
                onChange={(e) => setResponseContent(e.target.value)}
                className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline h-40"
                placeholder={responseType === 'json' ? '{"key": "value"}' : responseType === 'xml' ? '<root><element>value</element></root>' : 'Your response content here...'}
              />
            </div>
          </div>

          {/* Message Headers */}
          <div className="mb-6">
            <h2 className="text-lg font-semibold text-gray-700 mb-4">Message Headers</h2>
            
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div>
                <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="newHeaderName">
                  Name
                </label>
                <input
                  id="newHeaderName"
                  type="text"
                  value={newHeaderName}
                  onChange={(e) => setNewHeaderName(e.target.value)}
                  className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
                />
              </div>
              <div>
                <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="newHeaderValue">
                  Value
                </label>
                <input
                  id="newHeaderValue"
                  type="text"
                  value={newHeaderValue}
                  onChange={(e) => setNewHeaderValue(e.target.value)}
                  className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
                />
              </div>
              <div>
                <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="newHeaderType">
                  Type
                </label>
                <select
                  id="newHeaderType"
                  value={newHeaderType}
                  onChange={(e) => setNewHeaderType(e.target.value)}
                  className="shadow border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
                >
                  <option value="string">String</option>
                  <option value="boolean">Boolean</option>
                  <option value="int">Integer</option>
                  <option value="long">Long</option>
                  <option value="double">Double</option>
                  <option value="byte">Byte</option>
                </select>
              </div>
              <div className="flex items-end">
                <button
                  type="button"
                  onClick={handleAddHeader}
                  className="bg-blue-500 hover:bg-blue-600 text-white font-semibold py-2 px-4 rounded"
                >
                  Add Header
                </button>
              </div>
            </div>

            {headers.length > 0 ? (
              <div className="mt-4 overflow-x-auto">
                <table className="min-w-full bg-white border">
                  <thead>
                    <tr>
                      <th className="py-2 px-4 border-b text-left">Name</th>
                      <th className="py-2 px-4 border-b text-left">Value</th>
                      <th className="py-2 px-4 border-b text-left">Type</th>
                      <th className="py-2 px-4 border-b text-left">Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {headers.map((header, index) => (
                      <tr key={index}>
                        <td className="py-2 px-4 border-b">{header.name}</td>
                        <td className="py-2 px-4 border-b">{header.value}</td>
                        <td className="py-2 px-4 border-b">{header.type}</td>
                        <td className="py-2 px-4 border-b">
                          <button
                            type="button"
                            onClick={() => handleRemoveHeader(header.name)}
                            className="text-red-600 hover:text-red-900"
                          >
                            Remove
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="mt-4 text-gray-500 text-sm">
                No headers added yet. Headers will be included with the response message.
              </div>
            )}
          </div>

          {/* Status */}
          <div className="mb-6">
            <h2 className="text-lg font-semibold text-gray-700 mb-4">Status</h2>
            <div className="flex items-center">
              <input
                id="status"
                type="checkbox"
                checked={status}
                onChange={(e) => setStatus(e.target.checked)}
                className="h-4 w-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
              />
              <label className="ml-2 block text-gray-700" htmlFor="status">
                Activate stub immediately
              </label>
            </div>
          </div>

          {/* Submit Button */}
          <div className="flex items-center justify-between">
            <button
              type="button"
              onClick={() => navigate('/ibmmq')}
              className="bg-gray-300 hover:bg-gray-400 text-gray-800 font-semibold py-2 px-4 rounded focus:outline-none focus:shadow-outline"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isCreating || isUpdating}
              className={`bg-blue-500 hover:bg-blue-600 text-white font-semibold py-2 px-4 rounded focus:outline-none focus:shadow-outline ${
                (isCreating || isUpdating) ? 'opacity-50 cursor-not-allowed' : ''
              }`}
            >
              {isCreating || isUpdating ? 'Saving...' : isEditMode ? 'Update Stub' : 'Create Stub'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default IBMMQStubForm; 