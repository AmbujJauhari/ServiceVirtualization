import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { 
  useCreateSoapStubMutation, 
  useGetSoapStubByIdQuery, 
  useUpdateSoapStubMutation 
} from '../../../api/soapStubApi';

interface SoapStubFormProps {
  isEdit?: boolean;
}

const SoapStubForm: React.FC<SoapStubFormProps> = ({ isEdit = false }) => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  
  // API hooks
  const [createStub, { isLoading: isCreating }] = useCreateSoapStubMutation();
  const [updateStub, { isLoading: isUpdating }] = useUpdateSoapStubMutation();
  const { data: existingStub, isLoading: isLoadingStub } = useGetSoapStubByIdQuery(id ?? '', { 
    skip: !isEdit || !id 
  });
  
  // Form state
  const [activeSection, setActiveSection] = useState<'request' | 'response'>('request');
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    tags: [] as string[],
    status: 'ACTIVE',
    behindProxy: false,
    wsdlUrl: '',
    serviceName: '',
    portName: '',
    operationName: '',
    // Match conditions
    requestXPathFilters: [{ xpath: '', value: '', matchType: 'exact' }],
    requestNamespaces: [{ prefix: '', uri: '' }],
    requestBody: '',
    requestBodyMatchType: 'contains',
    // Response
    responseStatus: 200,
    responseBody: '',
    responseFault: false
  });
  
  // State for managing tag input
  const [tagInput, setTagInput] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Load existing stub data if in edit mode
  useEffect(() => {
    if (isEdit && existingStub) {
      try {
        // Extract data from existing stub to populate form
        const matchConditions = existingStub.matchConditions || {};
        const response = existingStub.response || {};
        
        setFormData({
          name: existingStub.name || '',
          description: existingStub.description || '',
          tags: existingStub.tags || [],
          status: existingStub.status || 'ACTIVE',
          behindProxy: existingStub.behindProxy || false,
          wsdlUrl: existingStub.wsdlUrl || '',
          serviceName: existingStub.serviceName || '',
          portName: existingStub.portName || '',
          operationName: existingStub.operationName || '',
          requestXPathFilters: (matchConditions.xpathFilters || []).length > 0 
            ? matchConditions.xpathFilters.map((f: any) => ({
                xpath: f.path || '',
                value: f.value || '',
                matchType: f.matchType || 'exact'
              }))
            : [{ xpath: '', value: '', matchType: 'exact' }],
          requestNamespaces: (matchConditions.namespaces || []).length > 0
            ? matchConditions.namespaces.map((n: any) => ({
                prefix: n.prefix || '',
                uri: n.uri || ''
              }))
            : [{ prefix: '', uri: '' }],
          requestBody: matchConditions.body || '',
          requestBodyMatchType: matchConditions.bodyMatchType || 'contains',
          responseStatus: response.status || 200,
          responseBody: response.body || '',
          responseFault: response.fault || false
        });
      } catch (error) {
        console.error('Error loading stub data:', error);
        setFormError('Error loading stub data. Some fields may not be populated correctly.');
      }
    }
  }, [isEdit, existingStub]);

  // Handle input changes
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value, type } = e.target;
    setFormData({
      ...formData,
      [name]: type === 'checkbox' ? (e.target as HTMLInputElement).checked : value
    });
  };

  // Handle tag input
  const handleTagInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setTagInput(e.target.value);
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

  // Handle Enter key in tag input
  const handleTagKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      addTag();
    }
  };

  // Remove a tag
  const removeTag = (tagToRemove: string) => {
    setFormData({
      ...formData,
      tags: formData.tags.filter(tag => tag !== tagToRemove)
    });
  };

  // Add/remove array items
  const addArrayItem = (field: 'requestXPathFilters' | 'requestNamespaces') => {
    const item = field === 'requestXPathFilters' 
      ? { xpath: '', value: '', matchType: 'exact' }
      : { prefix: '', uri: '' };
    
    setFormData({
      ...formData,
      [field]: [...formData[field], item]
    });
  };

  const removeArrayItem = (field: 'requestXPathFilters' | 'requestNamespaces', index: number) => {
    if (formData[field].length <= 1) return; // Keep at least one item
    
    setFormData({
      ...formData,
      [field]: formData[field].filter((_, i) => i !== index)
    });
  };

  // Handle array item changes
  const handleArrayItemChange = (
    field: 'requestXPathFilters' | 'requestNamespaces',
    index: number,
    key: string,
    value: string
  ) => {
    const newArray = [...formData[field]];
    newArray[index] = { ...newArray[index], [key]: value };
    
    setFormData({
      ...formData,
      [field]: newArray
    });
  };

  // Format form data for API submission
  const formatStubData = () => {
    // Filter out empty xpath filters and namespaces
    const validXPathFilters = formData.requestXPathFilters.filter(f => f.xpath.trim() !== '');
    const validNamespaces = formData.requestNamespaces.filter(n => n.prefix.trim() !== '' && n.uri.trim() !== '');
    
    // Format match conditions
    const matchConditions: Record<string, any> = {
      xpathFilters: validXPathFilters.map(f => ({
        path: f.xpath,
        value: f.value,
        matchType: f.matchType
      })),
      namespaces: validNamespaces.map(n => ({
        prefix: n.prefix,
        uri: n.uri
      }))
    };
    
    // Add body matching if provided
    if (formData.requestBody.trim()) {
      matchConditions.body = formData.requestBody;
      matchConditions.bodyMatchType = formData.requestBodyMatchType;
    }
    
    // Format response data
    const response: Record<string, any> = {
      status: formData.responseStatus,
      body: formData.responseBody,
      contentType: 'application/xml',
      fault: formData.responseFault
    };

    // Create the full stub data
    return {
      id: isEdit ? id : undefined,
      name: formData.name,
      description: formData.description,
      tags: formData.tags,
      status: formData.status,
      behindProxy: formData.behindProxy,
      protocol: 'SOAP',
      wsdlUrl: formData.wsdlUrl,
      serviceName: formData.serviceName,
      portName: formData.portName,
      operationName: formData.operationName,
      matchConditions,
      response
    };
  };

  // Handle form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Validate form
    if (!formData.name.trim()) {
      setFormError('Stub name is required');
      return;
    }
    
    if (!formData.serviceName.trim()) {
      setFormError('Service name is required');
      return;
    }
    
    if (!formData.operationName.trim()) {
      setFormError('Operation name is required');
      return;
    }
    
    try {
      setIsSubmitting(true);
      setFormError(null);
      
      const stubData = formatStubData();
      
      if (isEdit) {
        await updateStub(stubData).unwrap();
      } else {
        await createStub(stubData).unwrap();
      }
      
      // Navigate back to stubs list
      navigate('/soap');
    } catch (error: any) {
      console.error('Error submitting stub:', error);
      setFormError(error.data?.message || 'An error occurred while saving the stub');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Loading state
  if (isEdit && isLoadingStub) {
    return (
      <div className="flex justify-center items-center py-12">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      {/* Back link */}
      <Link to="/soap" className="text-primary-600 hover:text-primary-700 flex items-center mb-6">
        <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
        </svg>
        Back to SOAP Stubs
      </Link>

      <div className="bg-white shadow-md rounded-lg overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-gray-800">
            {isEdit ? 'Edit' : 'Create'} SOAP Stub
          </h2>
        </div>

        {/* Form error */}
        {formError && (
          <div className="bg-red-100 border-l-4 border-red-500 text-red-700 p-4 mb-6 mx-6 mt-4">
            <p>{formError}</p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="px-6 py-4">
          {/* Tabs */}
          <div className="flex border-b border-gray-200 mb-6">
            <button
              type="button"
              className={`py-2 px-4 font-medium text-sm focus:outline-none ${
                activeSection === 'request'
                  ? 'text-primary-600 border-b-2 border-primary-600'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
              onClick={() => setActiveSection('request')}
            >
              Request Matching
            </button>
            <button
              type="button"
              className={`py-2 px-4 font-medium text-sm focus:outline-none ${
                activeSection === 'response'
                  ? 'text-primary-600 border-b-2 border-primary-600'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
              onClick={() => setActiveSection('response')}
            >
              Response
            </button>
          </div>

          {/* Basic Info (Always visible) */}
          <div className="mb-6">
            <h3 className="text-lg font-medium text-gray-800 mb-4">Basic Information</h3>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                  required
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Status
                </label>
                <select
                  name="status"
                  value={formData.status}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                >
                  <option value="ACTIVE">Active</option>
                  <option value="INACTIVE">Inactive</option>
                </select>
              </div>
            </div>
            
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Description
              </label>
              <textarea
                name="description"
                value={formData.description}
                onChange={handleInputChange}
                rows={2}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-primary-500 focus:border-primary-500"
              />
            </div>
            
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Tags
              </label>
              <div className="flex flex-wrap items-center gap-2 mb-2">
                {formData.tags.map(tag => (
                  <span 
                    key={tag} 
                    className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800"
                  >
                    {tag}
                    <button
                      type="button"
                      onClick={() => removeTag(tag)}
                      className="ml-1.5 inline-flex text-blue-400 hover:text-blue-600 focus:outline-none"
                    >
                      <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </span>
                ))}
              </div>
              <div className="flex">
                <input
                  type="text"
                  value={tagInput}
                  onChange={handleTagInputChange}
                  onKeyDown={handleTagKeyDown}
                  placeholder="Add a tag"
                  className="flex-grow px-3 py-2 border border-gray-300 rounded-l-md focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                />
                <button
                  type="button"
                  onClick={addTag}
                  className="px-3 py-2 bg-gray-100 text-gray-700 border border-gray-300 border-l-0 rounded-r-md hover:bg-gray-200 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                >
                  Add
                </button>
              </div>
            </div>
            
            <div className="mb-4">
              <div className="flex items-center">
                <input
                  type="checkbox"
                  id="behindProxy"
                  name="behindProxy"
                  checked={formData.behindProxy}
                  onChange={handleInputChange}
                  className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                />
                <label htmlFor="behindProxy" className="ml-2 block text-sm text-gray-700">
                  Behind Proxy
                </label>
              </div>
              <p className="mt-1 text-xs text-gray-500">
                Check this if the service is accessed through a proxy server
              </p>
            </div>
          </div>

          {/* SOAP Service Info (Always visible) */}
          <div className="mb-6">
            <h3 className="text-lg font-medium text-gray-800 mb-4">SOAP Service Information</h3>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  WSDL URL
                </label>
                <input
                  type="text"
                  name="wsdlUrl"
                  value={formData.wsdlUrl}
                  onChange={handleInputChange}
                  placeholder="https://example.com/service.wsdl"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Service Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="serviceName"
                  value={formData.serviceName}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                  required
                />
              </div>
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Port Name
                </label>
                <input
                  type="text"
                  name="portName"
                  value={formData.portName}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Operation Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="operationName"
                  value={formData.operationName}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                  required
                />
              </div>
            </div>
          </div>

          {/* Request Matching Section */}
          {activeSection === 'request' && (
            <div className="mb-6">
              <h3 className="text-lg font-medium text-gray-800 mb-4">Request Matching</h3>
              
              {/* XPath Filters */}
              <div className="mb-4">
                <div className="flex justify-between items-center mb-2">
                  <label className="block text-sm font-medium text-gray-700">
                    XPath Filters
                  </label>
                  <button
                    type="button"
                    onClick={() => addArrayItem('requestXPathFilters')}
                    className="text-primary-600 hover:text-primary-700 text-sm font-medium focus:outline-none"
                  >
                    + Add Filter
                  </button>
                </div>
                
                {formData.requestXPathFilters.map((filter, index) => (
                  <div key={index} className="flex flex-col md:flex-row gap-2 mb-2 p-2 border border-gray-200 rounded-md">
                    <div className="flex-1">
                      <label className="block text-xs text-gray-500 mb-1">XPath</label>
                      <input
                        type="text"
                        value={filter.xpath}
                        onChange={(e) => handleArrayItemChange('requestXPathFilters', index, 'xpath', e.target.value)}
                        placeholder="//soap:Envelope/soap:Body/ns:GetWeather"
                        className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      />
                    </div>
                    <div className="flex-1">
                      <label className="block text-xs text-gray-500 mb-1">Value</label>
                      <input
                        type="text"
                        value={filter.value}
                        onChange={(e) => handleArrayItemChange('requestXPathFilters', index, 'value', e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      />
                    </div>
                    <div className="w-32">
                      <label className="block text-xs text-gray-500 mb-1">Match Type</label>
                      <select
                        value={filter.matchType}
                        onChange={(e) => handleArrayItemChange('requestXPathFilters', index, 'matchType', e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      >
                        <option value="exact">Exact</option>
                        <option value="contains">Contains</option>
                        <option value="regex">Regex</option>
                      </select>
                    </div>
                    <div className="flex items-end">
                      <button
                        type="button"
                        onClick={() => removeArrayItem('requestXPathFilters', index)}
                        className="px-2 py-2 text-red-500 hover:text-red-700 focus:outline-none"
                        title="Remove"
                      >
                        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
              
              {/* XML Namespaces */}
              <div className="mb-4">
                <div className="flex justify-between items-center mb-2">
                  <label className="block text-sm font-medium text-gray-700">
                    XML Namespaces
                  </label>
                  <button
                    type="button"
                    onClick={() => addArrayItem('requestNamespaces')}
                    className="text-primary-600 hover:text-primary-700 text-sm font-medium focus:outline-none"
                  >
                    + Add Namespace
                  </button>
                </div>
                
                {formData.requestNamespaces.map((namespace, index) => (
                  <div key={index} className="flex flex-col md:flex-row gap-2 mb-2 p-2 border border-gray-200 rounded-md">
                    <div className="w-32">
                      <label className="block text-xs text-gray-500 mb-1">Prefix</label>
                      <input
                        type="text"
                        value={namespace.prefix}
                        onChange={(e) => handleArrayItemChange('requestNamespaces', index, 'prefix', e.target.value)}
                        placeholder="soap"
                        className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      />
                    </div>
                    <div className="flex-1">
                      <label className="block text-xs text-gray-500 mb-1">URI</label>
                      <input
                        type="text"
                        value={namespace.uri}
                        onChange={(e) => handleArrayItemChange('requestNamespaces', index, 'uri', e.target.value)}
                        placeholder="http://schemas.xmlsoap.org/soap/envelope/"
                        className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      />
                    </div>
                    <div className="flex items-end">
                      <button
                        type="button"
                        onClick={() => removeArrayItem('requestNamespaces', index)}
                        className="px-2 py-2 text-red-500 hover:text-red-700 focus:outline-none"
                        title="Remove"
                      >
                        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
              
              {/* Request Body */}
              <div className="mb-4">
                <div className="flex justify-between items-center mb-2">
                  <label className="block text-sm font-medium text-gray-700">
                    Request Body Sample
                  </label>
                  <select
                    name="requestBodyMatchType"
                    value={formData.requestBodyMatchType}
                    onChange={handleInputChange}
                    className="px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                  >
                    <option value="contains">Contains</option>
                    <option value="exact">Exact Match</option>
                    <option value="regex">Regex</option>
                  </select>
                </div>
                <textarea
                  name="requestBody"
                  value={formData.requestBody}
                  onChange={handleInputChange}
                  rows={8}
                  placeholder="<soap:Envelope xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'>\n  <soap:Body>\n    <ns:GetWeather xmlns:ns='http://example.com'>\n      <ns:location>New York</ns:location>\n    </ns:GetWeather>\n  </soap:Body>\n</soap:Envelope>"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md font-mono text-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                />
                <p className="mt-1 text-xs text-gray-500">
                  XML request body to match against. You can use this along with XPath filters or as a standalone matcher.
                </p>
              </div>
            </div>
          )}
          
          {/* Response Section */}
          {activeSection === 'response' && (
            <div className="mb-6">
              <h3 className="text-lg font-medium text-gray-800 mb-4">Response Configuration</h3>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Response Status
                  </label>
                  <input
                    type="number"
                    name="responseStatus"
                    value={formData.responseStatus}
                    onChange={handleInputChange}
                    min="100"
                    max="599"
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                  />
                </div>
                
                <div className="flex items-end">
                  <div className="flex items-center h-10">
                    <input
                      type="checkbox"
                      id="responseFault"
                      name="responseFault"
                      checked={formData.responseFault}
                      onChange={handleInputChange}
                      className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                    />
                    <label htmlFor="responseFault" className="ml-2 block text-sm text-gray-700">
                      Return as SOAP Fault
                    </label>
                  </div>
                </div>
              </div>
              
              {/* Response Body */}
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Response Body
                </label>
                <textarea
                  name="responseBody"
                  value={formData.responseBody}
                  onChange={handleInputChange}
                  rows={12}
                  placeholder={formData.responseFault ? 
                    "<soap:Envelope xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'>\n  <soap:Body>\n    <soap:Fault>\n      <faultcode>soap:Server</faultcode>\n      <faultstring>Internal Server Error</faultstring>\n      <detail>\n        <error>Service unavailable</error>\n      </detail>\n    </soap:Fault>\n  </soap:Body>\n</soap:Envelope>" :
                    "<soap:Envelope xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'>\n  <soap:Body>\n    <ns:GetWeatherResponse xmlns:ns='http://example.com'>\n      <ns:temperature>72</ns:temperature>\n      <ns:conditions>Sunny</ns:conditions>\n    </ns:GetWeatherResponse>\n  </soap:Body>\n</soap:Envelope>"}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md font-mono text-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                />
                <p className="mt-1 text-xs text-gray-500">
                  XML response body to return when the request matches. Make sure it follows proper SOAP structure.
                </p>
              </div>
            </div>
          )}
          
          {/* Form Actions */}
          <div className="flex justify-end space-x-2 border-t border-gray-200 pt-4 mt-6">
            <Link
              to="/soap"
              className="px-4 py-2 border border-gray-300 text-gray-700 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
            >
              Cancel
            </Link>
            <button
              type="submit"
              disabled={isSubmitting}
              className={`px-4 py-2 bg-primary-600 text-white rounded-md hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 ${
                isSubmitting ? 'opacity-70 cursor-not-allowed' : ''
              }`}
            >
              {isSubmitting ? (
                <span className="flex items-center">
                  <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  Saving...
                </span>
              ) : (
                isEdit ? 'Update Stub' : 'Create Stub'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default SoapStubForm; 