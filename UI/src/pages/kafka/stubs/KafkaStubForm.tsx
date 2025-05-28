import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { 
  useGetKafkaStubByIdQuery, 
  useCreateKafkaStubMutation, 
  useUpdateKafkaStubMutation,
  KafkaStub,
  ContentFormat,
  ContentMatchType
} from '../../../api/kafkaApi';
import TextEditor from '../../../components/common/TextEditor';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`response-tabpanel-${index}`}
      aria-labelledby={`response-tab-${index}`}
      className="mt-4"
      {...other}
    >
      {value === index && <div>{children}</div>}
    </div>
  );
}

interface KafkaStubFormProps {
  mode: 'create' | 'edit';
}

const KafkaStubForm: React.FC<KafkaStubFormProps> = ({ mode }) => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState<'direct' | 'callback'>("direct");

  // Use RTK Query hooks
  const { data: existingStub, isLoading } = useGetKafkaStubByIdQuery(id || '', { 
    skip: mode === 'create' || !id 
  });
  const [createStub] = useCreateKafkaStubMutation();
  const [updateStub] = useUpdateKafkaStubMutation();

  // Header management
  const [callbackHeaders, setCallbackHeaders] = useState<{key: string, value: string}[]>([
    { key: '', value: '' }
  ]);

  // Schema Registry state for responses
  const [availableResponseSchemas, setAvailableResponseSchemas] = useState<any[]>([]);
  const [responseSchemaVersions, setResponseSchemaVersions] = useState<string[]>([]);
  const [isLoadingResponseSchemas, setIsLoadingResponseSchemas] = useState(false);
  const [responseSchemaValidationError, setResponseSchemaValidationError] = useState('');

  const [tagInput, setTagInput] = useState('');

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    requestTopic: '',
    requestContentFormat: 'JSON' as ContentFormat,
    requestContentMatcher: '',
    keyMatchType: ContentMatchType.NONE,
    keyPattern: '',
    contentMatchType: ContentMatchType.NONE,
    contentPattern: '',
    caseSensitive: false,
    responseTopic: '',
    responseContentFormat: 'JSON' as ContentFormat,
    responseKey: '',
    responseType: 'direct' as 'direct' | 'callback',
    responseContent: '',
    // Schema Registry for response validation
    useResponseSchemaRegistry: false,
    responseSchemaId: '',
    responseSchemaSubject: '',
    responseSchemaVersion: 'latest',
    latency: undefined as number | undefined,
    callbackUrl: '',
    status: 'active',
    tags: [] as string[],
  });

  // Update form when existing stub data is loaded
  useEffect(() => {
    if (existingStub) {
      setFormData(existingStub);
      
      // Set the active tab based on response type
      setActiveTab(existingStub.responseType as 'direct' | 'callback');
      
      // Convert callback headers to array format for editing
      if (existingStub.callbackHeaders) {
        const headerArray = Object.entries(existingStub.callbackHeaders).map(
          ([key, value]) => ({ key, value })
        );
        if (headerArray.length > 0) {
          setCallbackHeaders(headerArray);
        }
      }
    }
  }, [existingStub]);

  // Auto-enable response schema registry for Avro content type
  useEffect(() => {
    if (formData.responseContentFormat === 'AVRO') {
      setFormData(prev => ({ ...prev, useResponseSchemaRegistry: true }));
    }
  }, [formData.responseContentFormat]);

  // Fetch available response schemas when schema registry is enabled
  useEffect(() => {
    if (formData.useResponseSchemaRegistry && availableResponseSchemas.length === 0) {
      fetchAvailableResponseSchemas();
    }
  }, [formData.useResponseSchemaRegistry]);

  // Clear schema validation error when schema selection changes
  useEffect(() => {
    setResponseSchemaValidationError('');
  }, [formData.responseSchemaId, formData.responseSchemaSubject, formData.responseSchemaVersion]);

  // Validate response content when it changes
  useEffect(() => {
    if (formData.useResponseSchemaRegistry && formData.responseContent) {
      const timeoutId = setTimeout(() => {
        validateResponseContent();
      }, 500); // Debounce validation
      
      return () => clearTimeout(timeoutId);
    }
  }, [formData.responseContent, formData.responseSchemaId, formData.responseSchemaSubject, formData.responseSchemaVersion, formData.useResponseSchemaRegistry]);

  // Helper function to get placeholder text based on format
  const getMatcherPlaceholder = (format: ContentFormat): string => {
    switch (format) {
      case 'JSON':
        return '$.path.to.property == "value"';
      case 'XML':
        return '//element[@attribute="value"]';
      case 'AVRO':
        return 'record.field == "value"';
      default:
        return 'Enter matcher expression';
    }
  };

  const handleTabChange = (newValue: 'direct' | 'callback') => {
    setActiveTab(newValue);
    // Update response type based on selected tab
    setFormData(prev => ({
      ...prev,
      responseType: newValue
    }));
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleCheckboxChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, checked } = e.target;
    setFormData(prev => ({ ...prev, [name]: checked }));
  };

  const handleHeaderChange = (index: number, field: 'key' | 'value', value: string) => {
    const newHeaders = [...callbackHeaders];
    newHeaders[index][field] = value;
    setCallbackHeaders(newHeaders);
    
    // Update the headers object in formData
    const headersObject = callbackHeaders.reduce((acc, header) => {
      if (header.key && header.value) {
        acc[header.key] = header.value;
      }
      return acc;
    }, {} as Record<string, string>);
    
    setFormData(prev => ({
      ...prev,
      callbackHeaders: headersObject
    }));
  };

  const addHeader = () => {
    setCallbackHeaders([...callbackHeaders, { key: '', value: '' }]);
  };

  const removeHeader = (index: number) => {
    const newHeaders = [...callbackHeaders];
    newHeaders.splice(index, 1);
    setCallbackHeaders(newHeaders);
    
    // Update formData
    const headersObject = newHeaders.reduce((acc, header) => {
      if (header.key && header.value) {
        acc[header.key] = header.value;
      }
      return acc;
    }, {} as Record<string, string>);
    
    setFormData(prev => ({
      ...prev,
      callbackHeaders: headersObject
    }));
  };

  // Schema Registry functions for response validation
  const fetchAvailableResponseSchemas = async () => {
    setIsLoadingResponseSchemas(true);
    try {
      const response = await fetch(`${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/api/kafka/schemas`);
      if (response.ok) {
        const schemas = await response.json();
        setAvailableResponseSchemas(schemas);
      } else {
        console.warn('Failed to fetch schemas:', response.statusText);
      }
    } catch (error) {
      console.error('Error fetching schemas:', error);
    } finally {
      setIsLoadingResponseSchemas(false);
    }
  };

  const fetchResponseSchemaVersions = async (subject: string) => {
    try {
      const response = await fetch(`${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/api/kafka/schemas/${subject}/versions`);
      if (response.ok) {
        const versions = await response.json();
        setResponseSchemaVersions(versions);
      } else {
        console.warn('Failed to fetch schema versions:', response.statusText);
        setResponseSchemaVersions([]);
      }
    } catch (error) {
      console.error('Error fetching schema versions:', error);
      setResponseSchemaVersions([]);
    }
  };

  const validateResponseContent = async () => {
    if (!formData.useResponseSchemaRegistry || !formData.responseContent.trim()) {
      setResponseSchemaValidationError('');
      return;
    }

    try {
      const headers: Record<string, string> = {};
      
      if (formData.responseSchemaId) {
        headers['schema-id'] = formData.responseSchemaId;
      }
      if (formData.responseSchemaSubject) {
        headers['schema-subject'] = formData.responseSchemaSubject;
        headers['schema-version'] = formData.responseSchemaVersion;
      }

      const response = await fetch(`${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/api/kafka/validate-schema`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          message: formData.responseContent,
          headers: headers
        })
      });

      if (response.ok) {
        const result = await response.json();
        if (result.valid) {
          setResponseSchemaValidationError('');
        } else {
          setResponseSchemaValidationError(result.error || 'Schema validation failed');
        }
      } else {
        setResponseSchemaValidationError('Failed to validate schema');
      }
    } catch (error) {
      console.error('Schema validation error:', error);
      setResponseSchemaValidationError('Schema validation failed: ' + error.message);
    }
  };

  // Add a tag
  const addTag = () => {
    if (tagInput.trim() && !formData.tags.includes(tagInput.trim())) {
      setFormData({
        ...formData,
        tags: [...formData.tags, tagInput.trim()]
      });
      setTagInput('');
    }
  };

  // Remove a tag
  const removeTag = (tagToRemove: string) => {
    setFormData({
      ...formData,
      tags: formData.tags.filter(tag => tag !== tagToRemove)
    });
  };

   // Handle tag input
   const handleTagInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setTagInput(e.target.value);
  };

  // Handle Enter key in tag input
  const handleTagKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      addTag();
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);

    // Validate response schema if enabled
    if (formData.useResponseSchemaRegistry && formData.responseContent) {
      await validateResponseContent();
      if (responseSchemaValidationError) {
        setError('Please fix response schema validation errors before saving.');
        setSaving(false);
        return;
      }
    }

    try {
      if (mode === 'create') {
        await createStub(formData).unwrap();
      } else if (id) {
        await updateStub({ ...formData, id }).unwrap();
      }
      navigate('/kafka');
    } catch (err) {
      console.error('Error saving Kafka stub:', err);
      setError(`Failed to ${mode} Kafka stub. Please try again.`);
    } finally {
      setSaving(false);
    }
  };

  if (mode === 'edit' && isLoading) {
    return (
      <div className="p-6 text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-500 mx-auto"></div>
        <p className="mt-2 text-gray-600">Loading stub data...</p>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="flex items-center mb-6">
        <Link to="/kafka" className="text-primary-600 hover:text-primary-700 mr-4">
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
        </Link>
        <h2 className="text-lg font-medium text-gray-700">
          {mode === 'create' ? 'Create Kafka Stub' : 'Edit Kafka Stub'}
        </h2>
      </div>

      {error && (
        <div className="p-3 mb-4 bg-red-100 text-red-700 rounded">
          {error}
          <button 
            onClick={() => setError(null)} 
            className="ml-2 text-red-700 hover:text-red-900"
          >
            ✕
          </button>
        </div>
      )}

      <div className="bg-white rounded-lg shadow-md overflow-hidden">
        <form onSubmit={handleSubmit}>
          <div className="p-6 space-y-6">
            {/* Basic Information */}
            <div className="bg-gray-50 p-4 rounded-md border border-gray-200">
              <h3 className="text-lg font-medium text-gray-800 mb-4">Basic Information</h3>
              <div className="space-y-4">
                <div>
                  <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
                    Name *
                  </label>
                  <input
                    id="name"
                    name="name"
                    type="text"
                    required
                    value={formData.name || ''}
                    onChange={handleInputChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                    placeholder="Enter stub name"
                  />
                </div>
                
                <div>
                  <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
                    Description
                  </label>
                  <textarea
                    id="description"
                    name="description"
                    value={formData.description || ''}
                    onChange={handleInputChange}
                    rows={3}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                    placeholder="Enter stub description"
                  />
                </div>
              </div>
            </div>
            <div>
                <label htmlFor="tags" className="block text-sm font-medium text-gray-700 mb-1">
                  Tags
                </label>
                <div className="flex items-center">
                  <input
                    type="text"
                    id="tagInput"
                    value={tagInput}
                    onChange={handleTagInputChange}
                    onKeyDown={handleTagKeyDown}
                    className="flex-grow px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                    placeholder="Add tag and press Enter"
                  />
                  <button
                    type="button"
                    onClick={addTag}
                    className="ml-2 inline-flex items-center px-3 py-2 border border-transparent text-sm leading-4 font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                    </svg>
                  </button>
                </div>
                <div className="mt-2 flex flex-wrap gap-2">
                  {formData.tags.map((tag) => (
                    <span
                      key={tag}
                      className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-primary-100 text-primary-800"
                    >
                      {tag}
                      <button
                        type="button"
                        onClick={() => removeTag(tag)}
                        className="ml-1.5 inline-flex text-primary-500 focus:outline-none"
                      >
                        <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </button>
                    </span>
                  ))}
                </div>
              </div>

            {/* Request Configuration */}
            <div className="bg-gray-50 p-4 rounded-md border border-gray-200">
              <h3 className="text-lg font-medium text-gray-800 mb-4">Request Configuration</h3>
              <div className="space-y-4">
                <div>
                  <label htmlFor="requestTopic" className="block text-sm font-medium text-gray-700 mb-1">
                    Topic *
                  </label>
                  <input
                    id="requestTopic"
                    name="requestTopic"
                    type="text"
                    required
                    value={formData.requestTopic || ''}
                    onChange={handleInputChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                    placeholder="e.g., my-kafka-topic"
                  />
                </div>
                
                <div>
                  <label htmlFor="requestContentFormat" className="block text-sm font-medium text-gray-700 mb-1">
                    Content Format
                  </label>
                  <select
                    id="requestContentFormat"
                    name="requestContentFormat"
                    value={formData.requestContentFormat || 'JSON'}
                    onChange={handleInputChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                  >
                    <option value="JSON">JSON</option>
                    <option value="XML">XML</option>
                    <option value="AVRO">AVRO</option>
                    <option value="TEXT">TEXT</option>
                  </select>
                </div>
                
                <div>
                  <label htmlFor="keyMatchType" className="block text-sm font-medium text-gray-700 mb-1">
                    Key Match Type
                  </label>
                  <select
                    id="keyMatchType"
                    name="keyMatchType"
                    value={formData.keyMatchType}
                    onChange={(e) => setFormData(prev => ({ ...prev, keyMatchType: e.target.value as ContentMatchType }))}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                  >
                    <option value={ContentMatchType.NONE}>No key matching</option>
                    <option value={ContentMatchType.EXACT}>Exact match</option>
                    <option value={ContentMatchType.CONTAINS}>Contains text</option>
                    <option value={ContentMatchType.REGEX}>Regular expression</option>
                  </select>
                </div>
                
                {formData.keyMatchType !== ContentMatchType.NONE && (
                  <div>
                    <label htmlFor="keyPattern" className="block text-sm font-medium text-gray-700 mb-1">
                      Key Pattern
                    </label>
                    <input
                      id="keyPattern"
                      name="keyPattern"
                      type="text"
                      value={formData.keyPattern || ''}
                      onChange={handleInputChange}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      placeholder={
                        formData.keyMatchType === ContentMatchType.EXACT ? "user-123" :
                        formData.keyMatchType === ContentMatchType.CONTAINS ? "user" :
                        "user-\\d+"
                      }
                    />
                    <p className="mt-1 text-xs text-gray-500">
                      {formData.keyMatchType === ContentMatchType.EXACT && 'Messages with exactly this key will match'}
                      {formData.keyMatchType === ContentMatchType.CONTAINS && 'Messages with keys containing this text will match'}
                      {formData.keyMatchType === ContentMatchType.REGEX && 'Messages with keys matching this regular expression will match'}
                    </p>
                  </div>
                )}
                
                <div>
                  <label htmlFor="contentMatchType" className="block text-sm font-medium text-gray-700 mb-1">
                    Message Value Match Type
                  </label>
                  <select
                    id="contentMatchType"
                    name="contentMatchType"
                    value={formData.contentMatchType}
                    onChange={(e) => setFormData(prev => ({ ...prev, contentMatchType: e.target.value as ContentMatchType }))}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                  >
                    <option value={ContentMatchType.NONE}>No value matching</option>
                    <option value={ContentMatchType.EXACT}>Exact match</option>
                    <option value={ContentMatchType.CONTAINS}>Contains text</option>
                    <option value={ContentMatchType.REGEX}>Regular expression</option>
                  </select>
                  <p className="mt-1 text-xs text-gray-500">
                    How to match against the Kafka message value/content (the message payload)
                  </p>
                </div>
                
                {formData.contentMatchType !== ContentMatchType.NONE && (
                  <>
                    <div>
                      <label htmlFor="contentPattern" className="block text-sm font-medium text-gray-700 mb-1">
                        Message Value Pattern
                      </label>
                      <TextEditor
                        value={formData.contentPattern || ''}
                        onChange={(value) => setFormData(prev => ({ ...prev, contentPattern: value }))}
                        height="150px"
                        language="text"
                        placeholder={
                          formData.contentMatchType === ContentMatchType.EXACT ? "Complete message content to match exactly..." :
                          formData.contentMatchType === ContentMatchType.CONTAINS ? "Text that should be contained in the message..." :
                          "Regular expression pattern to match..."
                        }
                      />
                      <p className="mt-1 text-xs text-gray-500">
                        {formData.contentMatchType === ContentMatchType.EXACT && 'Messages with exactly this content will match'}
                        {formData.contentMatchType === ContentMatchType.CONTAINS && 'Messages containing this text will match'}
                        {formData.contentMatchType === ContentMatchType.REGEX && 'Messages matching this regular expression will match'}
                      </p>
                    </div>
                    
                    <div className="flex items-center">
                      <input
                        type="checkbox"
                        id="caseSensitive"
                        checked={formData.caseSensitive}
                        onChange={(e) => setFormData(prev => ({ ...prev, caseSensitive: e.target.checked }))}
                        className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                      />
                      <label htmlFor="caseSensitive" className="ml-2 block text-sm text-gray-700">
                        Case sensitive matching
                      </label>
                    </div>
                  </>
                )}
                
                <div className="border-t pt-4">
                  <h4 className="text-md font-medium text-gray-700 mb-2">Advanced: Format-Specific Value Matching</h4>
                  <div>
                    <label htmlFor="requestContentMatcher" className="block text-sm font-medium text-gray-700 mb-1">
                      Format-Specific Expression (Optional)
                    </label>
                    <textarea
                      id="requestContentMatcher"
                      name="requestContentMatcher"
                      value={formData.requestContentMatcher || ''}
                      onChange={handleInputChange}
                      rows={3}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      placeholder={getMatcherPlaceholder(formData.requestContentFormat as ContentFormat)}
                    />
                    <p className="mt-1 text-xs text-gray-500">
                      Advanced: JSONPath ($.field), XPath (//element), or Avro field expressions. 
                      This is evaluated in addition to the basic value pattern matching above.
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* Response Configuration */}
            <div className="bg-gray-50 p-4 rounded-md border border-gray-200">
              <h3 className="text-lg font-medium text-gray-800 mb-4">Response Configuration</h3>
              
              {/* Response Type Tabs */}
              <div className="border-b border-gray-200 mb-4">
                <nav className="-mb-px flex">
                  <button
                    type="button"
                    className={`py-2 px-4 border-b-2 font-medium text-sm ${
                      activeTab === 'direct'
                        ? 'border-primary-500 text-primary-600'
                        : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                    }`}
                    onClick={() => handleTabChange('direct')}
                  >
                    Direct Response
                  </button>
                  <button
                    type="button"
                    className={`py-2 px-4 border-b-2 font-medium text-sm ${
                      activeTab === 'callback'
                        ? 'border-primary-500 text-primary-600'
                        : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                    }`}
                    onClick={() => handleTabChange('callback')}
                  >
                    Callback
                  </button>
                </nav>
              </div>
              
              {/* Direct Response Tab Content */}
              {activeTab === 'direct' && (
                <div className="space-y-4">
                  <div>
                    <label htmlFor="responseTopic" className="block text-sm font-medium text-gray-700 mb-1">
                      Response Topic
                    </label>
                    <input
                      id="responseTopic"
                      name="responseTopic"
                      type="text"
                      value={formData.responseTopic || ''}
                      onChange={handleInputChange}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      placeholder="If different from request topic"
                    />
                    <p className="mt-1 text-sm text-gray-500">
                      Leave empty to use the request topic
                    </p>
                  </div>
                  
                  <div>
                    <label htmlFor="responseKey" className="block text-sm font-medium text-gray-700 mb-1">
                      Response Key
                    </label>
                    <input
                      id="responseKey"
                      name="responseKey"
                      type="text"
                      value={formData.responseKey || ''}
                      onChange={handleInputChange}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      placeholder="Leave empty to auto-generate UUID"
                    />
                    <p className="mt-1 text-sm text-gray-500">
                      Key for the response message. If empty, a UUID will be auto-generated.
                    </p>
                  </div>
                  
                  <div>
                    <label htmlFor="responseContentFormat" className="block text-sm font-medium text-gray-700 mb-1">
                      Response Format
                    </label>
                    <select
                      id="responseContentFormat"
                      name="responseContentFormat"
                      value={formData.responseContentFormat || 'JSON'}
                      onChange={handleInputChange}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                    >
                      <option value="JSON">JSON</option>
                      <option value="XML">XML</option>
                      <option value="AVRO">AVRO</option>
                      <option value="TEXT">TEXT</option>
                    </select>
                  </div>
                  
                  <div>
                    <label htmlFor="responseContent" className="block text-sm font-medium text-gray-700 mb-1">
                      Response Content
                    </label>
                    <textarea
                      id="responseContent"
                      name="responseContent"
                      value={formData.responseContent || ''}
                      onChange={handleInputChange}
                      rows={6}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      placeholder={`Enter response content in ${formData.responseContentFormat} format`}
                    />
                  </div>
                  
                  {/* Schema Registry Section for Response */}
                  <div className="border-t pt-4">
                    <div className="flex items-center mb-2">
                      <input
                        type="checkbox"
                        id="useResponseSchemaRegistry"
                        checked={formData.useResponseSchemaRegistry}
                        onChange={(e) => setFormData(prev => ({ ...prev, useResponseSchemaRegistry: e.target.checked }))}
                        className="mr-2"
                        disabled={formData.responseContentFormat === 'AVRO'}
                      />
                      <label htmlFor="useResponseSchemaRegistry" className="text-sm font-medium text-gray-700">
                        Use Schema Registry for Response Validation
                        {formData.responseContentFormat === 'AVRO' && <span className="text-xs text-gray-500 ml-1">(Required for Avro)</span>}
                      </label>
                    </div>

                    {formData.useResponseSchemaRegistry && (
                      <div className="bg-gray-50 p-4 rounded-md border border-gray-200 space-y-4">
                        {/* Schema Selection */}
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          <div>
                            <label htmlFor="responseSchemaSubject" className="block text-sm font-medium text-gray-700 mb-1">
                              Schema Subject
                            </label>
                            <select
                              id="responseSchemaSubject"
                              value={formData.responseSchemaSubject}
                              onChange={(e) => {
                                setFormData(prev => ({ ...prev, responseSchemaSubject: e.target.value }));
                                if (e.target.value) {
                                  fetchResponseSchemaVersions(e.target.value);
                                }
                              }}
                              className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                            >
                              <option value="">Select a schema subject...</option>
                              {isLoadingResponseSchemas ? (
                                <option disabled>Loading schemas...</option>
                              ) : availableResponseSchemas.length === 0 ? (
                                <option disabled>No schemas available</option>
                              ) : (
                                availableResponseSchemas.map((schema) => (
                                  <option key={schema.subject} value={schema.subject}>
                                    {schema.subject}
                                  </option>
                                ))
                              )}
                            </select>
                          </div>
                          
                          <div>
                            <label htmlFor="responseSchemaVersion" className="block text-sm font-medium text-gray-700 mb-1">
                              Schema Version
                            </label>
                            <select
                              id="responseSchemaVersion"
                              value={formData.responseSchemaVersion}
                              onChange={(e) => setFormData(prev => ({ ...prev, responseSchemaVersion: e.target.value }))}
                              className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                              disabled={!formData.responseSchemaSubject}
                            >
                              <option value="latest">Latest</option>
                              {responseSchemaVersions.map((version) => (
                                <option key={version} value={version}>
                                  Version {version}
                                </option>
                              ))}
                            </select>
                          </div>
                        </div>

                        {/* Alternative: Direct Schema ID */}
                        <div className="text-center text-gray-500 text-sm">or</div>
                        
                        <div className="w-full md:w-1/2">
                          <label htmlFor="responseSchemaId" className="block text-sm font-medium text-gray-700 mb-1">
                            Direct Schema ID (Optional)
                          </label>
                          <input
                            type="text"
                            id="responseSchemaId"
                            value={formData.responseSchemaId}
                            onChange={(e) => {
                              setFormData(prev => ({ ...prev, responseSchemaId: e.target.value }));
                              if (e.target.value) {
                                // Clear subject/version when using direct ID
                                setFormData(prev => ({ ...prev, responseSchemaSubject: '', responseSchemaVersion: 'latest' }));
                              }
                            }}
                            placeholder="e.g., 123"
                            className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                          />
                        </div>

                        {/* Schema Validation Error Display */}
                        {responseSchemaValidationError && (
                          <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded text-sm">
                            <strong>Response Schema Validation Error:</strong>
                            <pre className="mt-1 whitespace-pre-wrap">{responseSchemaValidationError}</pre>
                          </div>
                        )}

                        <div className="text-xs text-gray-600">
                          <p>• Select a schema subject and version, or provide a direct schema ID</p>
                          <p>• Response content will be validated against the selected schema</p>
                          <p>• Validation occurs automatically as you type</p>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )}
              
              {/* Callback Tab Content */}
              {activeTab === 'callback' && (
                <div className="space-y-4">
                  <div>
                    <label htmlFor="callbackUrl" className="block text-sm font-medium text-gray-700 mb-1">
                      Callback URL *
                    </label>
                    <input
                      id="callbackUrl"
                      name="callbackUrl"
                      type="url"
                      required={activeTab === 'callback'}
                      value={formData.callbackUrl || ''}
                      onChange={handleInputChange}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      placeholder="https://example.com/callback"
                    />
                  </div>
                  
                  <div>
                    <div className="flex justify-between items-center mb-2">
                      <label className="block text-sm font-medium text-gray-700">
                        Callback Headers
                      </label>
                      <button
                        type="button"
                        onClick={addHeader}
                        className="px-2 py-1 text-xs font-medium text-primary-600 hover:text-primary-700"
                      >
                        + Add Header
                      </button>
                    </div>
                    
                    {callbackHeaders.map((header, index) => (
                      <div key={index} className="flex space-x-2 mb-2">
                        <input
                          type="text"
                          value={header.key}
                          onChange={(e) => handleHeaderChange(index, 'key', e.target.value)}
                          className="w-1/3 px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                          placeholder="Header Name"
                        />
                        <input
                          type="text"
                          value={header.value}
                          onChange={(e) => handleHeaderChange(index, 'value', e.target.value)}
                          className="w-2/3 px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                          placeholder="Header Value"
                        />
                        <button
                          type="button"
                          onClick={() => removeHeader(index)}
                          className="px-2 py-1 text-gray-400 hover:text-gray-600"
                          title="Remove Header"
                        >
                          ✕
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
          
          {/* Form Actions */}
          <div className="px-6 py-4 bg-gray-50 flex justify-end space-x-4">
            <button
              type="button"
              onClick={() => navigate('/kafka')}
              className="px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className={`px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white ${
                saving ? 'bg-primary-400' : 'bg-primary-600 hover:bg-primary-700'
              } focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500`}
            >
              {saving ? 'Saving...' : mode === 'create' ? 'Create Stub' : 'Update Stub'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default KafkaStubForm; 