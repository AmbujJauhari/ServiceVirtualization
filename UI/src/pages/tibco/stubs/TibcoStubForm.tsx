import React, { useState, useEffect } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { useGetTibcoDestinationsQuery, useGetTibcoStubByIdQuery, useCreateTibcoStubMutation, useUpdateTibcoStubMutation, ContentMatchType, StubStatus } from '../../../api/tibcoApi';
import TextEditor from '../../../components/common/TextEditor';

interface TibcoStubFormProps {
  isEdit?: boolean;
}

interface ResponseHeader {
  key: string;
  value: string;
}

const TibcoStubForm: React.FC<TibcoStubFormProps> = ({ isEdit = false }) => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [createStub] = useCreateTibcoStubMutation();
  const [updateStub] = useUpdateTibcoStubMutation();
  const { data: destinations, isLoading: isLoadingDestinations } = useGetTibcoDestinationsQuery();
  const { data: existingStub, isLoading: isLoadingStub } = useGetTibcoStubByIdQuery(id!, { skip: !isEdit || !id });

  // Form state
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [requestDestinationType, setRequestDestinationType] = useState<'TOPIC' | 'QUEUE'>('TOPIC');
  const [requestDestinationName, setRequestDestinationName] = useState('');
  const [responseDestinationType, setResponseDestinationType] = useState<'TOPIC' | 'QUEUE'>('TOPIC');
  const [responseDestinationName, setResponseDestinationName] = useState('');
  const [messageSelector, setMessageSelector] = useState('');
  
  // Standardized content matching configuration
  const [contentMatchType, setContentMatchType] = useState<ContentMatchType>(ContentMatchType.NONE);
  const [contentPattern, setContentPattern] = useState('');
  const [caseSensitive, setCaseSensitive] = useState(false);
  const [priority, setPriority] = useState(0);
  
  const [responseType, setResponseType] = useState<'direct' | 'callback'>('direct');
  const [responseContent, setResponseContent] = useState('');
  const [responseHeaders, setResponseHeaders] = useState<ResponseHeader[]>([]);
  const [latency, setLatency] = useState(0);
  
  // UI state
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  // Load existing stub data if in edit mode
  useEffect(() => {
    if (isEdit && existingStub) {
      setName(existingStub.name);
      setDescription(existingStub.description || '');
      
      // Load request destination
      if (existingStub.requestDestination) {
        setRequestDestinationType(existingStub.requestDestination.type);
        setRequestDestinationName(existingStub.requestDestination.name);
      }
      
      // Load response destination
      if (existingStub.responseDestination) {
        setResponseDestinationType(existingStub.responseDestination.type);
        setResponseDestinationName(existingStub.responseDestination.name);
      } else if (existingStub.requestDestination) {
        // Default response destination to match request if not specified
        setResponseDestinationType(existingStub.requestDestination.type);
        setResponseDestinationName(existingStub.requestDestination.name);
      }
      
      setMessageSelector(existingStub.messageSelector || '');
      
      // Load standardized content matching configuration
      setContentMatchType(existingStub.contentMatchType || ContentMatchType.NONE);
      setContentPattern(existingStub.contentPattern || '');
      setCaseSensitive(existingStub.caseSensitive !== undefined ? existingStub.caseSensitive : false);
      setPriority(existingStub.priority || 0);
      
      setResponseType(existingStub.responseType === 'callback' ? 'callback' : 'direct');
      setResponseContent(existingStub.responseContent || '');
      setLatency(existingStub.latency || 0);
      
      // Load response headers if they exist
      if (existingStub.responseHeaders) {
        const headers: ResponseHeader[] = Object.entries(existingStub.responseHeaders).map(([key, value]) => ({
          key,
          value: value as string
        }));
        setResponseHeaders(headers);
      }
    }
  }, [isEdit, existingStub]);

  const handleAddHeader = () => {
    setResponseHeaders([...responseHeaders, { key: '', value: '' }]);
  };

  const handleRemoveHeader = (index: number) => {
    setResponseHeaders(responseHeaders.filter((_, i) => i !== index));
  };

  const handleHeaderChange = (index: number, field: 'key' | 'value', value: string) => {
    const updatedHeaders = [...responseHeaders];
    updatedHeaders[index][field] = value;
    setResponseHeaders(updatedHeaders);
  };

  const handleCancel = () => {
    navigate('/tibco');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');
    setIsSubmitting(true);

    try {
      // Convert headers array to object
      const headersObject: Record<string, string> = {};
      responseHeaders.forEach(header => {
        if (header.key.trim()) {
          headersObject[header.key.trim()] = header.value;
        }
      });

      const stubData = {
        id: isEdit ? id : undefined,
        name,
        description,
        requestDestination: {
          type: requestDestinationType,
          name: requestDestinationName
        },
        responseDestination: {
          type: responseDestinationType,
          name: responseDestinationName
        },
        messageSelector,
        
        // Standardized content matching configuration
        contentMatchType,
        contentPattern,
        caseSensitive,
        priority,
        
        responseType,
        responseContent,
        responseHeaders: headersObject,
        latency,
        status: StubStatus.ACTIVE
      };

      if (isEdit) {
        await updateStub(stubData).unwrap();
      } else {
        await createStub(stubData).unwrap();
      }

      navigate('/tibco');
    } catch (error) {
      console.error('Failed to save stub:', error);
      setErrorMessage('Failed to save stub. Please check your input and try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isEdit && isLoadingStub) {
    return <div className="p-6 text-center">Loading stub data...</div>;
  }

  const filteredDestinations = destinations?.filter(dest => dest.type === requestDestinationType) || [];

  return (
    <div className="p-6">
      <div className="mb-6">
        <h2 className="text-lg font-medium text-gray-700 mb-2">
          {isEdit ? 'Edit TIBCO Stub' : 'Create TIBCO Stub'}
        </h2>
        <nav className="text-sm text-gray-500">
          <Link to="/tibco" className="hover:text-gray-700">TIBCO</Link>
          <span className="mx-2">/</span>
          <span>{isEdit ? 'Edit Stub' : 'Create Stub'}</span>
        </nav>
      </div>

      {errorMessage && (
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-md">
          <p className="text-red-700">{errorMessage}</p>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Basic Information */}
        <div className="bg-white shadow rounded-lg p-6">
          <h3 className="text-md font-medium text-gray-900 mb-4">Basic Information</h3>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
                Name *
              </label>
              <input
                type="text"
                id="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                required
              />
            </div>

            <div>
              <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
                Description
              </label>
              <input
                type="text"
                id="description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
              />
            </div>
          </div>
        </div>

        {/* Destination Configuration */}
        <div className="bg-white shadow rounded-lg p-6">
          <h3 className="text-md font-medium text-gray-900 mb-4">Destination Configuration</h3>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Request Destination */}
            <div>
              <h4 className="text-sm font-medium text-gray-700 mb-3">Request Destination</h4>
              <div className="space-y-3">
                <div>
                  <label htmlFor="requestDestinationType" className="block text-sm font-medium text-gray-700 mb-1">
                    Type *
                  </label>
                  <select
                    id="requestDestinationType"
                    value={requestDestinationType}
                    onChange={(e) => setRequestDestinationType(e.target.value as 'TOPIC' | 'QUEUE')}
                    className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                    required
                  >
                    <option value="TOPIC">Topic</option>
                    <option value="QUEUE">Queue</option>
                  </select>
                </div>

                <div>
                  <label htmlFor="requestDestinationName" className="block text-sm font-medium text-gray-700 mb-1">
                    Name *
                  </label>
                  <input
                    type="text"
                    id="requestDestinationName"
                    value={requestDestinationName}
                    onChange={(e) => setRequestDestinationName(e.target.value)}
                    className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                    placeholder="Enter destination name"
                    required
                  />
                </div>
              </div>
            </div>

            {/* Response Destination */}
            <div>
              <h4 className="text-sm font-medium text-gray-700 mb-3">Response Destination</h4>
              <div className="space-y-3">
                <div>
                  <label htmlFor="responseDestinationType" className="block text-sm font-medium text-gray-700 mb-1">
                    Type *
                  </label>
                  <select
                    id="responseDestinationType"
                    value={responseDestinationType}
                    onChange={(e) => setResponseDestinationType(e.target.value as 'TOPIC' | 'QUEUE')}
                    className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                    required
                  >
                    <option value="TOPIC">Topic</option>
                    <option value="QUEUE">Queue</option>
                  </select>
                </div>

                <div>
                  <label htmlFor="responseDestinationName" className="block text-sm font-medium text-gray-700 mb-1">
                    Name *
                  </label>
                  <input
                    type="text"
                    id="responseDestinationName"
                    value={responseDestinationName}
                    onChange={(e) => setResponseDestinationName(e.target.value)}
                    className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                    placeholder="Enter destination name"
                    required
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Message Selector */}
        <div className="bg-white shadow rounded-lg p-6">
          <h3 className="text-md font-medium text-gray-900 mb-4">Message Selector</h3>
          
          <div>
            <label htmlFor="messageSelector" className="block text-sm font-medium text-gray-700 mb-1">
              Message Selector (JMS Selector)
            </label>
            <input
              type="text"
              id="messageSelector"
              value={messageSelector}
              onChange={(e) => setMessageSelector(e.target.value)}
              className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
              placeholder="e.g., MessageType = 'ORDER'"
            />
            <p className="mt-1 text-xs text-gray-500">
              Optional JMS message selector to filter incoming messages
            </p>
          </div>
        </div>

        {/* Content Matching Configuration */}
        <div className="bg-white shadow rounded-lg p-6">
          <h3 className="text-md font-medium text-gray-900 mb-4">Content Matching</h3>
          
          <div className="space-y-4">
            <div>
              <label htmlFor="contentMatchType" className="block text-sm font-medium text-gray-700 mb-1">
                Match Type
              </label>
              <select
                id="contentMatchType"
                value={contentMatchType}
                onChange={(e) => setContentMatchType(e.target.value as ContentMatchType)}
                className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
              >
                <option value={ContentMatchType.NONE}>No content matching</option>
                <option value={ContentMatchType.CONTAINS}>Message contains pattern</option>
                <option value={ContentMatchType.EXACT}>Message exactly matches pattern</option>
                <option value={ContentMatchType.REGEX}>Message matches regex pattern</option>
              </select>
            </div>

            {contentMatchType !== ContentMatchType.NONE && (
              <>
                <div>
                  <label htmlFor="contentPattern" className="block text-sm font-medium text-gray-700 mb-1">
                    Content Pattern
                  </label>
                  <TextEditor
                    value={contentPattern}
                    onChange={setContentPattern}
                    height="200px"
                    language="text"
                    placeholder="Enter the content pattern to match..."
                  />
                  <p className="mt-1 text-xs text-gray-500">
                    {contentMatchType === ContentMatchType.CONTAINS && 'Messages containing this text will match'}
                    {contentMatchType === ContentMatchType.EXACT && 'Messages with exactly this content will match'}
                    {contentMatchType === ContentMatchType.REGEX && 'Messages matching this regular expression will match'}
                  </p>
                </div>

                <div className="flex items-center">
                  <input
                    type="checkbox"
                    id="caseSensitive"
                    checked={caseSensitive}
                    onChange={(e) => setCaseSensitive(e.target.checked)}
                    className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                  />
                  <label htmlFor="caseSensitive" className="ml-2 block text-sm text-gray-700">
                    Case sensitive matching
                  </label>
                </div>
              </>
            )}

            <div>
              <label htmlFor="priority" className="block text-sm font-medium text-gray-700 mb-1">
                Priority
              </label>
              <input
                type="number"
                id="priority"
                value={priority}
                onChange={(e) => setPriority(parseInt(e.target.value) || 0)}
                min="0"
                max="100"
                className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
              />
              <p className="mt-1 text-xs text-gray-500">
                Higher priority stubs will be matched first (0-100, default: 0)
              </p>
            </div>
          </div>
        </div>

        {/* Response Configuration */}
        <div className="bg-white shadow rounded-lg p-6">
          <h3 className="text-md font-medium text-gray-900 mb-4">Response Configuration</h3>
          
          <div className="space-y-4">
            <div>
              <label htmlFor="responseType" className="block text-sm font-medium text-gray-700 mb-1">
                Response Type
              </label>
              <select
                id="responseType"
                value={responseType}
                onChange={(e) => setResponseType(e.target.value as 'direct' | 'callback')}
                className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
              >
                <option value="direct">Direct Response</option>
                <option value="callback">Callback (Future Enhancement)</option>
              </select>
            </div>

            <div>
              <label htmlFor="responseContent" className="block text-sm font-medium text-gray-700 mb-1">
                Response Content *
              </label>
              <TextEditor
                value={responseContent}
                onChange={setResponseContent}
                height="300px"
                language="json"
                placeholder="Enter the response message content..."
              />
            </div>

            <div>
              <label htmlFor="latency" className="block text-sm font-medium text-gray-700 mb-1">
                Response Latency (ms)
              </label>
              <input
                type="number"
                id="latency"
                value={latency}
                onChange={(e) => setLatency(parseInt(e.target.value) || 0)}
                min="0"
                className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                placeholder="0"
              />
              <p className="mt-1 text-xs text-gray-500">
                Artificial delay before sending the response (in milliseconds)
              </p>
            </div>

            {/* Response Headers */}
            <div>
              <div className="flex justify-between items-center mb-2">
                <label className="block text-sm font-medium text-gray-700">
                  Response Headers
                </label>
                <button
                  type="button"
                  onClick={handleAddHeader}
                  className="text-sm text-primary-600 hover:text-primary-700"
                >
                  + Add Header
                </button>
              </div>
              
              {responseHeaders.map((header, index) => (
                <div key={index} className="flex gap-2 mb-2">
                  <input
                    type="text"
                    placeholder="Header name"
                    value={header.key}
                    onChange={(e) => handleHeaderChange(index, 'key', e.target.value)}
                    className="flex-1 rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                  />
                  <input
                    type="text"
                    placeholder="Header value"
                    value={header.value}
                    onChange={(e) => handleHeaderChange(index, 'value', e.target.value)}
                    className="flex-1 rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                  />
                  <button
                    type="button"
                    onClick={() => handleRemoveHeader(index)}
                    className="px-3 py-2 text-red-600 hover:text-red-700"
                  >
                    Remove
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Actions */}
        <div className="flex justify-end space-x-3">
          <button
            type="button"
            onClick={handleCancel}
            className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="px-4 py-2 bg-primary-600 text-white rounded-md hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 disabled:opacity-50"
          >
            {isSubmitting ? 'Saving...' : (isEdit ? 'Update Stub' : 'Create Stub')}
          </button>
        </div>
      </form>
    </div>
  );
};

export default TibcoStubForm; 