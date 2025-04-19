import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { 
  useGetActiveMQStubQuery, 
  useCreateActiveMQStubMutation, 
  useUpdateActiveMQStubMutation,
  ActiveMQStub,
  MessageHeader,
  StubStatus,
  ContentMatchType,
  CreateStubErrorResponse
} from '../../../api/activemqApi';

interface ActiveMQStubFormProps {
  isEdit?: boolean;
}

/**
 * Form for creating and editing ActiveMQ stubs
 */
const ActiveMQStubForm: React.FC<ActiveMQStubFormProps> = ({ isEdit = false }) => {
  const { id } = useParams<{ id: string }>();
  const isEditMode = isEdit || Boolean(id);
  const navigate = useNavigate();

  // Error state
  const [error, setError] = useState<string | null>(null);

  // Fetch existing stub for edit mode
  const { data: existingStub, isLoading: isLoadingStub } = useGetActiveMQStubQuery(id ?? '', {
    skip: !isEditMode
  });

  // Mutations
  const [createStub, { isLoading: isCreating, error: createError }] = useCreateActiveMQStubMutation();
  const [updateStub, { isLoading: isUpdating }] = useUpdateActiveMQStubMutation();

  // Form state
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [destinationType, setDestinationType] = useState('queue');
  const [destinationName, setDestinationName] = useState('');
  const [selector, setSelector] = useState('');
  const [contentMatchType, setContentMatchType] = useState<ContentMatchType>(ContentMatchType.NONE);
  const [contentPattern, setContentPattern] = useState('');
  const [caseSensitive, setCaseSensitive] = useState(true);
  const [priority, setPriority] = useState(0);
  const [responseType, setResponseType] = useState('text');
  const [responseContent, setResponseContent] = useState('');
  const [latency, setLatency] = useState(0);
  const [headers, setHeaders] = useState<MessageHeader[]>([]);
  const [status, setStatus] = useState<StubStatus>(StubStatus.ACTIVE);

  // New header form fields
  const [newHeaderName, setNewHeaderName] = useState('');
  const [newHeaderValue, setNewHeaderValue] = useState('');
  const [newHeaderType, setNewHeaderType] = useState('string');

  // Populate form with existing data in edit mode
  useEffect(() => {
    if (existingStub) {
      setName(existingStub.name || '');
      setDescription(existingStub.description || '');
      setDestinationType(existingStub.destinationType || 'queue');
      setDestinationName(existingStub.destinationName || '');
      setSelector(existingStub.selector || '');
      setContentMatchType(existingStub.contentMatchType || ContentMatchType.NONE);
      setContentPattern(existingStub.contentPattern || '');
      setCaseSensitive(existingStub.caseSensitive ?? true);
      setPriority(existingStub.priority ?? 0);
      setResponseType(existingStub.responseType || 'text');
      setResponseContent(existingStub.responseContent || '');
      setLatency(existingStub.latency || 0);
      setHeaders(existingStub.headers || []);
      setStatus(existingStub.status ?? StubStatus.ACTIVE);
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
    setError(null);

    const stubData: Partial<ActiveMQStub> = {
      name,
      description,
      destinationType,
      destinationName,
      selector,
      contentMatchType,
      contentPattern,
      caseSensitive,
      priority,
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
        navigate('/activemq');
      } else {
        try {
          const result = await createStub(stubData).unwrap();
          navigate('/activemq');
        } catch (err: any) {
          if (err.status === 409) { // Conflict status code
            const errorResponse = err.data as CreateStubErrorResponse;
            setError(errorResponse.message || 'A stub with higher priority already exists for this destination.');
          } else {
            setError('An error occurred while creating the stub. Please try again.');
            console.error('Error creating stub:', err);
          }
        }
      }
    } catch (error) {
      setError('An error occurred while saving the stub. Please try again.');
      console.error('Error saving ActiveMQ stub:', error);
    }
  };

  if (isEditMode && isLoadingStub) {
    return <div className="text-center py-4">Loading...</div>;
  }

  return (
    <div className="container mx-auto p-4">
      <div className="bg-white shadow-md rounded-lg p-6">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">
          {isEditMode ? 'Edit ActiveMQ Stub' : 'Create ActiveMQ Stub'}
        </h1>

        {error && (
          <div className="bg-red-100 border-l-4 border-red-500 text-red-700 p-4 mb-6" role="alert">
            <p className="font-bold">Error</p>
            <p>{error}</p>
          </div>
        )}

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

          {/* Destination Configuration */}
          <div className="mb-6">
            <h2 className="text-lg font-semibold text-gray-700 mb-4">Destination Configuration</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="destinationType">
                  Destination Type*
                </label>
                <select
                  id="destinationType"
                  value={destinationType}
                  onChange={(e) => setDestinationType(e.target.value)}
                  className="shadow border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
                  required
                >
                  <option value="queue">Queue</option>
                  <option value="topic">Topic</option>
                </select>
              </div>
              <div>
                <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="destinationName">
                  Destination Name*
                </label>
                <input
                  id="destinationName"
                  type="text"
                  value={destinationName}
                  onChange={(e) => setDestinationName(e.target.value)}
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
                placeholder="e.g. type='important' AND priority > 5"
                className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
              />
              <p className="text-sm text-gray-500 mt-1">
                JMS selector expression to filter which messages this stub responds to.
              </p>
            </div>
            <div className="mt-4">
              <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="priority">
                Priority
              </label>
              <input
                id="priority"
                type="number"
                min="0"
                max="100"
                value={priority}
                onChange={(e) => setPriority(parseInt(e.target.value) || 0)}
                className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
              />
              <p className="text-sm text-gray-500 mt-1">
                Priority for matching (higher values = higher priority). New stubs cannot have a priority lower than existing stubs.
              </p>
            </div>
          </div>

          {/* Content Matching Configuration */}
          <div className="mb-6">
            <h2 className="text-lg font-semibold text-gray-700 mb-4">Content Matching</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="contentMatchType">
                  Match Type
                </label>
                <select
                  id="contentMatchType"
                  value={contentMatchType}
                  onChange={(e) => setContentMatchType(e.target.value as ContentMatchType)}
                  className="shadow border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
                >
                  <option value={ContentMatchType.NONE}>No Content Matching</option>
                  <option value={ContentMatchType.CONTAINS}>Contains Text</option>
                  <option value={ContentMatchType.EXACT}>Exact Match</option>
                  <option value={ContentMatchType.REGEX}>Regular Expression</option>
                </select>
                <p className="text-sm text-gray-500 mt-1">
                  How to match the message content.
                </p>
              </div>
              <div>
                <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="caseSensitive">
                  Case Sensitivity
                </label>
                <div className="flex items-center">
                  <input
                    id="caseSensitive"
                    type="checkbox"
                    checked={caseSensitive}
                    onChange={(e) => setCaseSensitive(e.target.checked)}
                    className="h-4 w-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                  />
                  <label className="ml-2 block text-gray-700" htmlFor="caseSensitive">
                    Case sensitive matching
                  </label>
                </div>
              </div>
            </div>
            
            {contentMatchType !== ContentMatchType.NONE && (
              <div className="mt-4">
                <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="contentPattern">
                  Content Pattern
                </label>
                <textarea
                  id="contentPattern"
                  value={contentPattern}
                  onChange={(e) => setContentPattern(e.target.value)}
                  className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline h-20"
                  placeholder={contentMatchType === ContentMatchType.REGEX ? '^\\s*<order>.*</order>\\s*$' : contentMatchType === ContentMatchType.EXACT ? 'Exact message content to match' : 'Text to search for in the message'}
                />
                <p className="text-sm text-gray-500 mt-1">
                  {contentMatchType === ContentMatchType.REGEX ? 
                    'Regular expression pattern to match against message content.' : 
                    contentMatchType === ContentMatchType.CONTAINS ?
                    'Text pattern that must be contained in the message content.' :
                    'Exact text that the message content must match.'}
                </p>
              </div>
            )}
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
            <div>
              <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="status">
                Status
              </label>
              <select
                id="status"
                value={status}
                onChange={(e) => setStatus(e.target.value as StubStatus)}
                className="shadow border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
              >
                <option value={StubStatus.ACTIVE}>Active</option>
                <option value={StubStatus.INACTIVE}>Inactive</option>
                <option value={StubStatus.DRAFT}>Draft</option>
                <option value={StubStatus.ARCHIVED}>Archived</option>
              </select>
              <p className="text-sm text-gray-500 mt-1">
                Set the status of this stub. Only Active stubs will receive and respond to messages.
              </p>
            </div>
          </div>

          {/* Submit Button */}
          <div className="flex items-center justify-between">
            <button
              type="button"
              onClick={() => navigate('/activemq')}
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

export default ActiveMQStubForm; 