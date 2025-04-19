import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { 
  useGetKafkaStubByIdQuery, 
  useCreateKafkaStubMutation, 
  useUpdateKafkaStubMutation,
  KafkaStub,
  ContentFormat
} from '../../../api/kafkaApi';

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

  const [formData, setFormData] = useState<Partial<KafkaStub>>({
    name: '',
    description: '',
    userId: '',
    requestTopic: '',
    responseTopic: '',
    requestContentFormat: 'JSON',
    requestContentMatcher: '',
    responseContentFormat: 'JSON',
    keyPattern: '',
    valuePattern: '',
    responseType: 'direct',
    responseContent: '',
    callbackUrl: '',
    callbackHeaders: {},
    status: 'inactive',
    tags: []
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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);

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
                    placeholder="Regular expression to match message keys"
                  />
                </div>
                
                <div>
                  <label htmlFor="valuePattern" className="block text-sm font-medium text-gray-700 mb-1">
                    Value Pattern
                  </label>
                  <input
                    id="valuePattern"
                    name="valuePattern"
                    type="text"
                    value={formData.valuePattern || ''}
                    onChange={handleInputChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                    placeholder="Regular expression to match message content"
                  />
                </div>
                
                <div>
                  <label htmlFor="requestContentMatcher" className="block text-sm font-medium text-gray-700 mb-1">
                    Content Matcher Expression
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
            
            {/* Status Configuration */}
            <div className="bg-gray-50 p-4 rounded-md border border-gray-200">
              <h3 className="text-lg font-medium text-gray-800 mb-4">Status</h3>
              <div className="space-y-4">
                <div>
                  <label htmlFor="status" className="block text-sm font-medium text-gray-700 mb-1">
                    Stub Status
                  </label>
                  <select
                    id="status"
                    name="status"
                    value={formData.status || 'inactive'}
                    onChange={handleInputChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                  >
                    <option value="active">Active</option>
                    <option value="inactive">Inactive</option>
                  </select>
                </div>
              </div>
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