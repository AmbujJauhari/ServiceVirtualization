import React, { useState, useEffect } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { useGetTibcoDestinationsQuery, useGetTibcoStubByIdQuery, useCreateTibcoStubMutation, useUpdateTibcoStubMutation } from '../../../api/tibcoApi';
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
  const [responseType, setResponseType] = useState<'direct' | 'callback'>('direct');
  const [responseContent, setResponseContent] = useState('');
  const [responseHeaders, setResponseHeaders] = useState<ResponseHeader[]>([]);
  const [callbackUrl, setCallbackUrl] = useState('');
  const [tags, setTags] = useState<string[]>([]);
  const [tagInput, setTagInput] = useState('');
  
  // UI state
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  // Load existing stub data if in edit mode
  useEffect(() => {
    if (isEdit && existingStub) {
      setName(existingStub.name);
      setDescription(existingStub.description || '');
      
      // Handle both new and old format
      if (existingStub.requestDestination) {
        // New format with separate request/response destinations
        setRequestDestinationType(existingStub.requestDestination.type);
        setRequestDestinationName(existingStub.requestDestination.name);
        
        if (existingStub.responseDestination) {
          setResponseDestinationType(existingStub.responseDestination.type);
          setResponseDestinationName(existingStub.responseDestination.name);
        } else {
          // Default response destination to match request if not specified
          setResponseDestinationType(existingStub.requestDestination.type);
          setResponseDestinationName(existingStub.requestDestination.name);
        }
      } else if (existingStub.destinationType && existingStub.destinationName) {
        // Old format with single destination
        setRequestDestinationType(existingStub.destinationType);
        setRequestDestinationName(existingStub.destinationName);
        setResponseDestinationType(existingStub.destinationType);
        setResponseDestinationName(existingStub.destinationName);
      }
      
      setMessageSelector(existingStub.matchConditions?.messageSelector || '');
      setResponseType(existingStub.response?.type || 'direct');
      
      if (existingStub.response?.type === 'callback') {
        setCallbackUrl(existingStub.response?.callbackUrl || '');
        setResponseContent('');
      } else {
        setResponseContent(existingStub.response?.content || '');
        setCallbackUrl('');
      }
      
      // Load response headers if they exist
      if (existingStub.responseHeaders) {
        const headers: ResponseHeader[] = Object.entries(existingStub.responseHeaders).map(([key, value]) => ({
          key,
          value: value as string
        }));
        setResponseHeaders(headers);
      }
      
      setTags(existingStub.tags || []);
    }
  }, [isEdit, existingStub]);

  const handleAddTag = () => {
    if (tagInput.trim() && !tags.includes(tagInput.trim())) {
      setTags([...tags, tagInput.trim()]);
      setTagInput('');
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setTags(tags.filter(tag => tag !== tagToRemove));
  };

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
        matchConditions: {
          messageSelector
        },
        response: responseType === 'direct' 
          ? {
              type: 'direct',
              content: responseContent
            } 
          : {
              type: 'callback',
              callbackUrl
            },
        responseHeaders: headersObject,
        tags,
        status: 'ACTIVE'
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
      <div className="flex items-center mb-6">
        <Link to="/tibco" className="text-primary-600 hover:text-primary-700 mr-4">
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
        </Link>
        <h2 className="text-lg font-medium text-gray-700">
          {isEdit ? 'Edit TIBCO Message Stub' : 'Create TIBCO Message Stub'}
        </h2>
      </div>

      {errorMessage && (
        <div className="mb-4 p-3 bg-red-100 border border-red-200 text-red-700 rounded">
          {errorMessage}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div className="mb-6">
          <h3 className="text-md font-medium text-gray-700 mb-2">Basic Information</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
                Name *
              </label>
              <input
                id="name"
                type="text"
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
                id="description"
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
              />
            </div>
          </div>
        </div>

        <div className="mb-6">
          <h3 className="text-md font-medium text-gray-700 mb-2">Request Destination</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Request Destination Type *
              </label>
              <div className="flex gap-2">
                <button
                  type="button"
                  className={`flex-1 py-2 px-4 rounded ${
                    requestDestinationType === 'TOPIC'
                      ? 'bg-primary-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                  onClick={() => setRequestDestinationType('TOPIC')}
                >
                  Topic
                </button>
                <button
                  type="button"
                  className={`flex-1 py-2 px-4 rounded ${
                    requestDestinationType === 'QUEUE'
                      ? 'bg-primary-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                  onClick={() => setRequestDestinationType('QUEUE')}
                >
                  Queue
                </button>
              </div>
            </div>
            <div>
              <label htmlFor="requestDestinationName" className="block text-sm font-medium text-gray-700 mb-1">
                Request Destination Name *
              </label>
              <input
                id="requestDestinationName"
                type="text"
                value={requestDestinationName}
                onChange={(e) => setRequestDestinationName(e.target.value)}
                placeholder={`Enter ${requestDestinationType.toLowerCase()} name`}
                className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                required
              />
            </div>
          </div>
        </div>

        <div className="mb-6">
          <h3 className="text-md font-medium text-gray-700 mb-2">Response Destination</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Response Destination Type *
              </label>
              <div className="flex gap-2">
                <button
                  type="button"
                  className={`flex-1 py-2 px-4 rounded ${
                    responseDestinationType === 'TOPIC'
                      ? 'bg-primary-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                  onClick={() => setResponseDestinationType('TOPIC')}
                >
                  Topic
                </button>
                <button
                  type="button"
                  className={`flex-1 py-2 px-4 rounded ${
                    responseDestinationType === 'QUEUE'
                      ? 'bg-primary-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                  onClick={() => setResponseDestinationType('QUEUE')}
                >
                  Queue
                </button>
              </div>
            </div>
            <div>
              <label htmlFor="responseDestinationName" className="block text-sm font-medium text-gray-700 mb-1">
                Response Destination Name *
              </label>
              <input
                id="responseDestinationName"
                type="text"
                value={responseDestinationName}
                onChange={(e) => setResponseDestinationName(e.target.value)}
                placeholder={`Enter ${responseDestinationType.toLowerCase()} name`}
                className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                required
              />
            </div>
          </div>
        </div>

        <div className="mb-6">
          <h3 className="text-md font-medium text-gray-700 mb-2">Message Matching</h3>
          <div>
            <label htmlFor="messageSelector" className="block text-sm font-medium text-gray-700 mb-1">
              Message Selector (JMS Selector Syntax)
            </label>
            <input
              id="messageSelector"
              type="text"
              value={messageSelector}
              onChange={(e) => setMessageSelector(e.target.value)}
              placeholder="e.g. JMSType='Order' AND OrderAmount > 1000"
              className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
            />
            <p className="mt-1 text-sm text-gray-500">
              Leave empty to match all messages sent to this destination
            </p>
          </div>
        </div>

        <div className="mb-6">
          <h3 className="text-md font-medium text-gray-700 mb-2">Response</h3>
          <div className="mb-3">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Response Type *
            </label>
            <div className="flex gap-2">
              <button
                type="button"
                className={`py-2 px-4 rounded ${
                  responseType === 'direct'
                    ? 'bg-primary-600 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
                onClick={() => setResponseType('direct')}
              >
                Direct Response
              </button>
              <button
                type="button"
                className={`py-2 px-4 rounded ${
                  responseType === 'callback'
                    ? 'bg-primary-600 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
                onClick={() => setResponseType('callback')}
              >
                Callback
              </button>
            </div>
          </div>

          {responseType === 'direct' ? (
            <>
              {/* Response Headers */}
              <div className="mb-4">
                <div className="flex justify-between items-center mb-2">
                  <label className="block text-sm font-medium text-gray-700">
                    Response Headers
                  </label>
                  <button
                    type="button"
                    onClick={handleAddHeader}
                    className="text-primary-600 hover:text-primary-900 text-sm font-medium flex items-center"
                  >
                    <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                    </svg>
                    Add Header
                  </button>
                </div>
                
                {responseHeaders.map((header, index) => (
                  <div key={index} className="grid grid-cols-12 gap-2 mb-2">
                    <div className="col-span-5">
                      <input
                        type="text"
                        placeholder="Header name"
                        value={header.key}
                        onChange={(e) => handleHeaderChange(index, 'key', e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      />
                    </div>
                    <div className="col-span-6">
                      <input
                        type="text"
                        placeholder="Header value"
                        value={header.value}
                        onChange={(e) => handleHeaderChange(index, 'value', e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      />
                    </div>
                    <div className="col-span-1">
                      <button
                        type="button"
                        onClick={() => handleRemoveHeader(index)}
                        className="text-red-600 hover:text-red-900 p-2"
                      >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
              
              <div>
                <label htmlFor="responseContent" className="block text-sm font-medium text-gray-700 mb-1">
                  Response Content *
                </label>
                <div className="bg-gray-50 p-4 rounded-md border border-gray-200 mb-2">
                  <TextEditor
                    value={responseContent}
                    onChange={setResponseContent}
                    language="plaintext"
                    height="200px"
                    placeholder="Enter response message content here"
                  />
                </div>
              </div>
            </>
          ) : (
            <div>
              <label htmlFor="callbackUrl" className="block text-sm font-medium text-gray-700 mb-1">
                Callback URL *
              </label>
              <input
                id="callbackUrl"
                type="url"
                value={callbackUrl}
                onChange={(e) => setCallbackUrl(e.target.value)}
                placeholder="https://example.com/callback"
                className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                required={responseType === 'callback'}
              />
              <p className="mt-1 text-sm text-gray-500">
                The original message will be forwarded to this URL
              </p>
            </div>
          )}
        </div>

        <div className="mb-6">
          <h3 className="text-md font-medium text-gray-700 mb-2">Tags</h3>
          <div className="flex flex-wrap items-center gap-2 mb-2">
            {tags.map((tag, index) => (
              <span 
                key={index} 
                className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-primary-100 text-primary-800"
              >
                {tag}
                <button
                  type="button"
                  onClick={() => handleRemoveTag(tag)}
                  className="ml-1.5 inline-flex text-primary-500 focus:outline-none"
                >
                  <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12"></path>
                  </svg>
                </button>
              </span>
            ))}
            <div className="flex">
              <input
                type="text"
                value={tagInput}
                onChange={(e) => setTagInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), handleAddTag())}
                placeholder="Add tag"
                className="rounded-l-md border border-gray-300 shadow-sm px-3 py-1 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
              />
              <button
                type="button"
                onClick={handleAddTag}
                className="bg-gray-100 text-gray-700 px-2 py-1 rounded-r-md border border-l-0 border-gray-300 hover:bg-gray-200"
              >
                Add
              </button>
            </div>
          </div>
        </div>

        <div className="flex justify-end">
          <button
            type="button"
            onClick={handleCancel}
            className="mr-3 bg-white text-gray-700 px-4 py-2 rounded-md border border-gray-300 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isSubmitting || !name || !requestDestinationName || !responseDestinationName || 
              (responseType === 'direct' && !responseContent) || 
              (responseType === 'callback' && !callbackUrl)}
            className={`bg-primary-600 text-white px-4 py-2 rounded-md hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 ${
              isSubmitting || !name || !requestDestinationName || !responseDestinationName || 
              (responseType === 'direct' && !responseContent) || 
              (responseType === 'callback' && !callbackUrl) ? 'opacity-50 cursor-not-allowed' : ''
            }`}
          >
            {isSubmitting ? 'Saving...' : isEdit ? 'Update Stub' : 'Create Stub'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default TibcoStubForm; 