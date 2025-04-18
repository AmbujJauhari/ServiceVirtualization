import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useCreateStubMutation, useGetStubByIdQuery, useUpdateStubMutation, useGetStubsQuery } from '../../../api/stubApi';

interface StubFormProps {
  isEdit?: boolean;
}

const StubForm: React.FC<StubFormProps> = ({ isEdit = false }) => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  
  // API hooks
  const [createStub, { isLoading: isCreating, error: createError }] = useCreateStubMutation();
  const [updateStub, { isLoading: isUpdating, error: updateError }] = useUpdateStubMutation();
  const { data: existingStub, isLoading: isLoadingStub } = useGetStubByIdQuery(id ?? '', { 
    skip: !isEdit || !id 
  });
  const { data: stubs } = useGetStubsQuery();
  
  // State for available methods
  const [availableMethods, setAvailableMethods] = useState<string[]>([
    'GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS', 'ANY'
  ]);
  
  // Form state
  const [activeSection, setActiveSection] = useState<'request' | 'response'>('request');
  const [responseType, setResponseType] = useState<'direct' | 'callback'>('direct');
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    priority: 1,
    tags: [] as string[],
    status: 'ACTIVE',
    behindProxy: false,
    requestMethod: 'GET',
    requestUrl: '',
    requestUrlMatchType: 'exact',
    requestHeaders: [{ key: '', value: '', matchType: 'exact' }],
    requestQueryParams: [{ key: '', value: '', matchType: 'exact' }],
    requestBody: '',
    requestBodyMatchType: 'exact',
    responseStatus: 200,
    responseHeaders: [{ key: '', value: '' }],
    responseBody: '',
    responseBodyType: 'json',
    callbackUrl: '',
    callbackMethod: 'POST'
  });
  
  // Extract unique methods from stubs data
  useEffect(() => {
    if (stubs && stubs.length > 0) {
      // Get methods from existing stubs
      const methodsFromStubs = stubs
        .map(stub => stub.matchConditions?.method || 'ANY')
        .filter((method, index, self) => self.indexOf(method) === index);
      
      // Combine with standard methods and remove duplicates
      const standardMethods = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS', 'ANY'];
      const allMethods = [...new Set([...methodsFromStubs, ...standardMethods])].sort();
      
      setAvailableMethods(allMethods);
    }
  }, [stubs]);
  
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
        
        // Determine response type
        const isCallbackResponse = !!response.callback;
        
        setResponseType(isCallbackResponse ? 'callback' : 'direct');
        
        // Extract callback data if present
        let callbackUrl = '';
        let callbackMethod = 'POST';
        
        if (isCallbackResponse && response.callback) {
          const callback = response.callback;
          callbackUrl = callback.url || '';
          callbackMethod = callback.method || 'POST';
        }
        
        // Map existing data to form structure
        setFormData({
          name: existingStub.name || '',
          description: existingStub.description || '',
          priority: matchConditions.priority || 1,
          tags: existingStub.tags || [],
          status: existingStub.status || 'ACTIVE',
          behindProxy: existingStub.behindProxy || false,
          requestMethod: matchConditions.method || 'GET',
          requestUrl: matchConditions.url || '',
          requestUrlMatchType: matchConditions.urlMatchType || 'exact',
          requestHeaders: (matchConditions.headers || []).length > 0 
            ? matchConditions.headers.map((h: any) => ({
                key: h.name || '',
                value: h.value || '',
                matchType: h.matchType || 'exact'
              }))
            : [{ key: '', value: '', matchType: 'exact' }],
          requestQueryParams: (matchConditions.queryParams || []).length > 0
            ? matchConditions.queryParams.map((p: any) => ({
                key: p.name || '',
                value: p.value || '',
                matchType: p.matchType || 'exact'
              }))
            : [{ key: '', value: '', matchType: 'exact' }],
          requestBody: matchConditions.body || '',
          requestBodyMatchType: matchConditions.bodyMatchType || 'exact',
          responseStatus: response.status || 200,
          responseHeaders: (response.headers || []).length > 0
            ? response.headers.map((h: any) => ({
                key: h.name || '',
                value: h.value || ''
              }))
            : [{ key: '', value: '' }],
          responseBody: response.body || '',
          responseBodyType: response.contentType 
            ? response.contentType.includes('json') 
              ? 'json' 
              : response.contentType.includes('xml') 
                ? 'xml' 
                : response.contentType.includes('html') 
                  ? 'html' 
                  : 'text'
            : 'json',
          callbackUrl,
          callbackMethod
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
  const addArrayItem = (field: 'requestHeaders' | 'responseHeaders' | 'requestQueryParams') => {
    const item = field === 'responseHeaders' 
      ? { key: '', value: '' }
      : { key: '', value: '', matchType: 'exact' };
    
    setFormData({
      ...formData,
      [field]: [...formData[field], item]
    });
  };

  const removeArrayItem = (field: 'requestHeaders' | 'responseHeaders' | 'requestQueryParams') => {
    setFormData({
      ...formData,
      [field]: formData[field].filter((_, i) => i !== 0)
    });
  };

  // Handle array item changes
  const handleArrayItemChange = (
    field: 'requestHeaders' | 'responseHeaders' | 'requestQueryParams',
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
    // Filter out empty headers and query params
    const validRequestHeaders = formData.requestHeaders.filter(h => h.key.trim() !== '');
    const validResponseHeaders = formData.responseHeaders.filter(h => h.key.trim() !== '');
    const validQueryParams = formData.requestQueryParams.filter(p => p.key.trim() !== '');
    
    // Format request match conditions
    const matchConditions: Record<string, any> = {
      method: formData.requestMethod,
      url: formData.requestUrl,
      urlMatchType: formData.requestUrlMatchType,
      priority: formData.priority,
      headers: validRequestHeaders.map(h => ({
        name: h.key,
        value: h.value,
        matchType: h.matchType
      })),
      queryParams: validQueryParams.map(p => ({
        name: p.key,
        value: p.value,
        matchType: p.matchType
      })),
      behindProxy: formData.behindProxy
    };
    
    // Add body matching if provided
    if (formData.requestBody.trim()) {
      matchConditions.body = formData.requestBody;
      matchConditions.bodyMatchType = formData.requestBodyMatchType;
    }
    
    // Format response data
    let response: Record<string, any> = {};
    
    if (responseType === 'direct') {
      // Determine content type
      let contentType = 'application/json';
      if (formData.responseBodyType === 'xml') contentType = 'application/xml';
      if (formData.responseBodyType === 'text') contentType = 'text/plain';
      if (formData.responseBodyType === 'html') contentType = 'text/html';
      
      response = {
        status: Number(formData.responseStatus),
        headers: validResponseHeaders.map(h => ({
          name: h.key,
          value: h.value
        })),
        body: formData.responseBody,
        contentType
      };
    } else {
      // Format callback data
      const callback: Record<string, any> = {
        url: formData.callbackUrl,
        method: formData.callbackMethod
      };
      
      response = {
        callback: callback
      };
    }
    
    // Construct the stub data object
    return {
      name: formData.name,
      description: formData.description,
      protocol: 'HTTP', // Hardcoded for REST stubs
      tags: formData.tags,
      behindProxy: formData.behindProxy,
      status: "ACTIVE", // Default to active
      matchConditions,
      response
    };
  };

  // Handle form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setIsSubmitting(true);
    
    try {
      const stubData = formatStubData();
      
      if (isEdit && id) {
        // Update existing stub (cast as any to avoid type errors with missing fields)
        await updateStub({ ...stubData, id } as any).unwrap();
      } else {
        // Create new stub
        await createStub(stubData).unwrap();
      }
      
      // Redirect back to stubs list on success
      navigate('/rest');
    } catch (error) {
      console.error('Error submitting stub:', error);
      setFormError('Failed to save stub. Please check your form and try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Display loading state while fetching stub data in edit mode
  if (isEdit && isLoadingStub) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="bg-white rounded-lg shadow-md p-6 text-center">
          <p className="text-gray-600">Loading stub data...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex justify-between items-center mb-6">
        <Link to="/rest" className="text-primary-600 hover:text-primary-700 flex items-center">
          <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          Back to REST Dashboard
        </Link>
      </div>

      <div className="bg-white rounded-lg shadow-md overflow-hidden">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-gray-800">{isEdit ? 'Edit Stub' : 'Create New Stub'}</h2>
        </div>

        {/* Display any form errors */}
        {(formError || createError || updateError) && (
          <div className="p-4 bg-red-50 border-l-4 border-red-500">
            <p className="text-red-700">
              {formError || 'An error occurred while saving the stub. Please try again.'}
            </p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="p-6">
          {/* Basic Information */}
          <div className="mb-8">
            <h3 className="text-lg font-medium text-gray-700 mb-4">Basic Information</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
                  Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  id="name"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
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
                  name="description"
                  value={formData.description}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                />
              </div>
              <div>
                <label htmlFor="priority" className="block text-sm font-medium text-gray-700 mb-1">
                  Priority
                </label>
                <input
                  type="number"
                  id="priority"
                  name="priority"
                  value={formData.priority}
                  onChange={handleInputChange}
                  min="1"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                />
                <p className="mt-1 text-xs text-gray-500">Higher priority stubs are matched first</p>
              </div>
              <div>
                <div className="flex flex-col space-y-4">
                  <div>
                    <label htmlFor="behindProxy" className="flex items-center text-sm font-medium text-gray-700">
                      <input
                        type="checkbox"
                        id="behindProxy"
                        name="behindProxy"
                        checked={formData.behindProxy}
                        onChange={handleInputChange}
                        className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded mr-2"
                      />
                      Access through proxy
                    </label>
                    <p className="mt-1 text-xs text-gray-500 ml-6">Enable if this stub will be accessed through a proxy server</p>
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
            </div>
          </div>

          {/* Request & Response Tabs */}
          <div className="mb-8">
            <div className="border-b border-gray-200">
              <nav className="-mb-px flex space-x-8" aria-label="Tabs">
                <button
                  type="button"
                  className={`whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm 
                    ${activeSection === 'request' 
                      ? 'border-primary-500 text-primary-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  }`}
                  onClick={() => setActiveSection('request')}
                >
                  Request Matching
                </button>
                <button
                  type="button"
                  className={`whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm 
                    ${activeSection === 'response' 
                      ? 'border-primary-500 text-primary-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  }`}
                  onClick={() => setActiveSection('response')}
                >
                  Response Definition
                </button>
              </nav>
            </div>
          </div>

          {/* Request Matching Section */}
          {activeSection === 'request' && (
            <div className="mb-8">
              <div className="mb-6">
                <h4 className="text-md font-medium text-gray-700 mb-4">URL & Method</h4>
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                  <div>
                    <label htmlFor="requestMethod" className="block text-sm font-medium text-gray-700 mb-1">
                      Method <span className="text-red-500">*</span>
                    </label>
                    <select
                      id="requestMethod"
                      name="requestMethod"
                      value={formData.requestMethod}
                      onChange={handleInputChange}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      required
                    >
                      {availableMethods.map(method => (
                        <option key={method} value={method}>{method}</option>
                      ))}
                    </select>
                  </div>
                  <div className="md:col-span-2">
                    <label htmlFor="requestUrl" className="block text-sm font-medium text-gray-700 mb-1">
                      URL Path <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="text"
                      id="requestUrl"
                      name="requestUrl"
                      value={formData.requestUrl}
                      onChange={handleInputChange}
                      placeholder="/api/users"
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      required
                    />
                  </div>
                  <div>
                    <label htmlFor="requestUrlMatchType" className="block text-sm font-medium text-gray-700 mb-1">
                      Match Type
                    </label>
                    <select
                      id="requestUrlMatchType"
                      name="requestUrlMatchType"
                      value={formData.requestUrlMatchType}
                      onChange={handleInputChange}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                    >
                      <option value="exact">Exact</option>
                      <option value="regex">Regex</option>
                      <option value="glob">Glob</option>
                    </select>
                  </div>
                </div>
              </div>

              <div className="mb-6">
                <div className="flex justify-between items-center mb-2">
                  <h4 className="text-md font-medium text-gray-700">Headers</h4>
                  <button
                    type="button"
                    onClick={() => addArrayItem('requestHeaders')}
                    className="text-primary-600 hover:text-primary-900 text-sm font-medium flex items-center"
                  >
                    <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                    </svg>
                    Add Header
                  </button>
                </div>
                {formData.requestHeaders.map((header, index) => (
                  <div key={index} className="grid grid-cols-12 gap-2 mb-2">
                    <div className="col-span-5">
                      <input
                        type="text"
                        placeholder="Header name"
                        value={header.key}
                        onChange={(e) => handleArrayItemChange('requestHeaders', index, 'key', e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      />
                    </div>
                    <div className="col-span-5">
                      <input
                        type="text"
                        placeholder="Header value"
                        value={header.value}
                        onChange={(e) => handleArrayItemChange('requestHeaders', index, 'value', e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      />
                    </div>
                    <div className="col-span-1">
                      <select
                        value={header.matchType}
                        onChange={(e) => handleArrayItemChange('requestHeaders', index, 'matchType', e.target.value)}
                        className="w-full px-2 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      >
                        <option value="exact">Exact</option>
                        <option value="regex">Regex</option>
                        <option value="contains">Contains</option>
                      </select>
                    </div>
                    <div className="col-span-1">
                      <button
                        type="button"
                        onClick={() => removeArrayItem('requestHeaders')}
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

              <div className="mb-6">
                <div className="flex justify-between items-center mb-2">
                  <h4 className="text-md font-medium text-gray-700">Query Parameters</h4>
                  <button
                    type="button"
                    onClick={() => addArrayItem('requestQueryParams')}
                    className="text-primary-600 hover:text-primary-900 text-sm font-medium flex items-center"
                  >
                    <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                    </svg>
                    Add Parameter
                  </button>
                </div>
                {formData.requestQueryParams.map((param, index) => (
                  <div key={index} className="grid grid-cols-12 gap-2 mb-2">
                    <div className="col-span-5">
                      <input
                        type="text"
                        placeholder="Parameter name"
                        value={param.key}
                        onChange={(e) => handleArrayItemChange('requestQueryParams', index, 'key', e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      />
                    </div>
                    <div className="col-span-5">
                      <input
                        type="text"
                        placeholder="Parameter value"
                        value={param.value}
                        onChange={(e) => handleArrayItemChange('requestQueryParams', index, 'value', e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      />
                    </div>
                    <div className="col-span-1">
                      <select
                        value={param.matchType}
                        onChange={(e) => handleArrayItemChange('requestQueryParams', index, 'matchType', e.target.value)}
                        className="w-full px-2 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      >
                        <option value="exact">Exact</option>
                        <option value="regex">Regex</option>
                        <option value="contains">Contains</option>
                      </select>
                    </div>
                    <div className="col-span-1">
                      <button
                        type="button"
                        onClick={() => removeArrayItem('requestQueryParams')}
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

              <div className="mb-6">
                <h4 className="text-md font-medium text-gray-700 mb-2">Request Body</h4>
                <div className="grid grid-cols-1 gap-4">
                  <div>
                    <textarea
                      id="requestBody"
                      name="requestBody"
                      value={formData.requestBody}
                      onChange={handleInputChange}
                      rows={5}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 font-mono"
                      placeholder='{
  "example": "JSON body"
}'
                    ></textarea>
                  </div>
                  <div className="flex items-center">
                    <label className="text-sm font-medium text-gray-700 mr-2">Match Type: </label>
                    <select
                      id="requestBodyMatchType"
                      name="requestBodyMatchType"
                      value={formData.requestBodyMatchType}
                      onChange={handleInputChange}
                      className="px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                    >
                      <option value="exact">Exact</option>
                      <option value="json">JSON Schema</option>
                      <option value="jsonpath">JSONPath</option>
                      <option value="xpath">XPath</option>
                      <option value="contains">Contains</option>
                      <option value="regex">Regex</option>
                    </select>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Response Definition Section */}
          {activeSection === 'response' && (
            <div className="mb-8">
              <div className="mb-6">
                <h4 className="text-md font-medium text-gray-700 mb-4">Response Type</h4>
                <div className="flex space-x-4 mb-6">
                  <div className="flex items-center">
                    <input
                      id="direct-response"
                      name="response-type"
                      type="radio"
                      checked={responseType === 'direct'}
                      onChange={() => setResponseType('direct')}
                      className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300"
                    />
                    <label htmlFor="direct-response" className="ml-2 block text-sm font-medium text-gray-700">
                      Define Response
                    </label>
                  </div>
                  <div className="flex items-center">
                    <input
                      id="callback-response"
                      name="response-type"
                      type="radio"
                      checked={responseType === 'callback'}
                      onChange={() => setResponseType('callback')}
                      className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300"
                    />
                    <label htmlFor="callback-response" className="ml-2 block text-sm font-medium text-gray-700">
                      Use Callback URL
                    </label>
                  </div>
                </div>

                {responseType === 'direct' ? (
                  <>
                    <h4 className="text-md font-medium text-gray-700 mb-4">Response Status</h4>
                    <div>
                      <label htmlFor="responseStatus" className="block text-sm font-medium text-gray-700 mb-1">
                        Status Code <span className="text-red-500">*</span>
                      </label>
                      <select
                        id="responseStatus"
                        name="responseStatus"
                        value={formData.responseStatus}
                        onChange={handleInputChange}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                        required
                      >
                        <option value="200">200 - OK</option>
                        <option value="201">201 - Created</option>
                        <option value="204">204 - No Content</option>
                        <option value="400">400 - Bad Request</option>
                        <option value="401">401 - Unauthorized</option>
                        <option value="403">403 - Forbidden</option>
                        <option value="404">404 - Not Found</option>
                        <option value="500">500 - Internal Server Error</option>
                        <option value="503">503 - Service Unavailable</option>
                      </select>
                    </div>
                  </>
                ) : (
                  <div className="bg-gray-50 p-4 rounded-md mt-4">
                    <div className="mb-4">
                      <label htmlFor="callbackUrl" className="block text-sm font-medium text-gray-700 mb-1">
                        Callback URL <span className="text-red-500">*</span>
                      </label>
                      <input
                        type="text"
                        id="callbackUrl"
                        name="callbackUrl"
                        value={formData.callbackUrl}
                        onChange={handleInputChange}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                        placeholder="https://your-service.com/webhook"
                        required={responseType === 'callback'}
                      />
                      <p className="mt-1 text-xs text-gray-500">The URL that will be called when this stub is matched</p>
                    </div>
                  </div>
                )}
              </div>

              {responseType === 'direct' && (
                <>
                  <div className="mb-6">
                    <div className="flex justify-between items-center mb-2">
                      <h4 className="text-md font-medium text-gray-700">Response Headers</h4>
                      <button
                        type="button"
                        onClick={() => addArrayItem('responseHeaders')}
                        className="text-primary-600 hover:text-primary-900 text-sm font-medium flex items-center"
                      >
                        <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                        </svg>
                        Add Header
                      </button>
                    </div>
                    {formData.responseHeaders.map((header, index) => (
                      <div key={index} className="grid grid-cols-12 gap-2 mb-2">
                        <div className="col-span-5">
                          <input
                            type="text"
                            placeholder="Header name"
                            value={header.key}
                            onChange={(e) => handleArrayItemChange('responseHeaders', index, 'key', e.target.value)}
                            className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                          />
                        </div>
                        <div className="col-span-6">
                          <input
                            type="text"
                            placeholder="Header value"
                            value={header.value}
                            onChange={(e) => handleArrayItemChange('responseHeaders', index, 'value', e.target.value)}
                            className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                          />
                        </div>
                        <div className="col-span-1">
                          <button
                            type="button"
                            onClick={() => removeArrayItem('responseHeaders')}
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

                  <div className="mb-6">
                    <h4 className="text-md font-medium text-gray-700 mb-2">Response Body</h4>
                    <div className="mb-2">
                      <label htmlFor="responseBodyType" className="block text-sm font-medium text-gray-700 mb-1">
                        Content Type
                      </label>
                      <select
                        id="responseBodyType"
                        name="responseBodyType"
                        value={formData.responseBodyType}
                        onChange={handleInputChange}
                        className="w-full md:w-1/4 px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                      >
                        <option value="json">application/json</option>
                        <option value="xml">application/xml</option>
                        <option value="text">text/plain</option>
                        <option value="html">text/html</option>
                      </select>
                    </div>
                    <div>
                      <textarea
                        id="responseBody"
                        name="responseBody"
                        value={formData.responseBody}
                        onChange={handleInputChange}
                        rows={8}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 font-mono"
                        placeholder={
                          formData.responseBodyType === 'json' 
                            ? '{\n  "message": "Success",\n  "data": {\n    "id": 123,\n    "name": "Example"\n  }\n}'
                            : formData.responseBodyType === 'xml'
                            ? '<response>\n  <message>Success</message>\n  <data>\n    <id>123</id>\n    <name>Example</name>\n  </data>\n</response>'
                            : 'Response body content'
                        }
                      ></textarea>
                    </div>
                  </div>
                </>
              )}
            </div>
          )}

          <div className="flex justify-end">
            <Link 
              to="/rest"
              className="mr-4 bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
            >
              Cancel
            </Link>
            <button
              type="submit"
              disabled={isSubmitting || isCreating || isUpdating}
              className={`bg-primary-600 py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 ${
                (isSubmitting || isCreating || isUpdating) ? 'opacity-75 cursor-not-allowed' : ''
              }`}
            >
              {(isSubmitting || isCreating || isUpdating) ? (
                <>
                  <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white inline-block" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  {isEdit ? 'Updating...' : 'Creating...'}
                </>
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

export default StubForm; 