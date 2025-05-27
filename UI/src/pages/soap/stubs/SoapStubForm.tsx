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
  const [responseType, setResponseType] = useState<'static' | 'callback'>('static');
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    tags: [] as string[],
    status: 'ACTIVE',
    behindProxy: false,
    url: '',
    soapAction: '',
    // Request matching
    requestBody: '',
    requestBodyMatchType: 'contains',
    // Response
    responseStatus: 200,
    responseBody: '',
    // Webhook
    callbackUrl: ''
  });
  
  // State for managing tag input
  const [tagInput, setTagInput] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Load existing stub data if in edit mode
  useEffect(() => {
    if (isEdit && existingStub) {
      try {
        const matchConditions = existingStub.matchConditions || {};
        const response = existingStub.response || {};
        
        // Check if this is a callback response
        const isCallbackResponse = response.callback && response.callback.url;
        
        // Extract callback data if present
        let callbackUrl = '';
        
        if (isCallbackResponse && response.callback) {
          const callback = response.callback;
          callbackUrl = callback.url || '';
          setResponseType('callback');
        } else if (existingStub.webhookUrl) {
          // Check for top-level webhookUrl field (new format)
          callbackUrl = existingStub.webhookUrl;
          setResponseType('callback');
        }
        
        setFormData({
          name: existingStub.name || '',
          description: existingStub.description || '',
          tags: existingStub.tags || [],
          status: existingStub.status || 'ACTIVE',
          behindProxy: existingStub.behindProxy || false,
          url: existingStub.url || '',
          soapAction: existingStub.soapAction || '',
          requestBody: matchConditions.body || '',
          requestBodyMatchType: matchConditions.bodyMatchType || 'contains',
          responseStatus: response.status || 200,
          responseBody: response.body || '',
          callbackUrl: callbackUrl
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

  // Format form data for API submission
  const formatStubData = () => {
    // Format match conditions
    const matchConditions: Record<string, any> = {};
    
    // Add body matching if provided
    if (formData.requestBody.trim()) {
      matchConditions.body = formData.requestBody;
      matchConditions.bodyMatchType = formData.requestBodyMatchType;
    }
    
    // Format response data
    const response: Record<string, any> = {
      status: formData.responseStatus
    };
    
    // Add response body
    if (formData.responseBody.trim()) {
      response.body = formData.responseBody;
    }
    
    // Add callback configuration if callback type is selected
    if (responseType === 'callback' && formData.callbackUrl.trim()) {
      response.callback = {
        url: formData.callbackUrl,
        method: 'POST'
      };
    }
    
    // Construct the stub data object
    const stubData = {
      name: formData.name,
      description: formData.description,
      protocol: 'SOAP', // Hardcoded for SOAP stubs
      tags: formData.tags,
      behindProxy: formData.behindProxy,
      status: formData.status,
      url: formData.url,
      soapAction: formData.soapAction || undefined,
      matchConditions,
      response
    };
    
    // Extract webhook URL for backend compatibility
    if (responseType === 'callback' && formData.callbackUrl) {
      (stubData as any).webhookUrl = formData.callbackUrl;
    }
    
    return stubData;
  };

  // Handle form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setIsSubmitting(true);

    try {
      // Validate required fields
      if (!formData.name.trim()) {
        throw new Error('Name is required');
      }
      if (!formData.url.trim()) {
        throw new Error('URL is required');
      }

      const stubData = formatStubData();

      if (isEdit && id) {
        // Update existing stub
        await updateStub({ id, ...stubData }).unwrap();
      } else {
        // Create new stub
        await createStub(stubData).unwrap();
      }

      // Navigate back to stub list
      navigate('/soap');
    } catch (error: any) {
      console.error('Error saving stub:', error);
      setFormError(error.message || 'Failed to save stub. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoadingStub) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto p-6">
      <div className="bg-white shadow-lg rounded-lg">
        <div className="px-6 py-4 border-b border-gray-200">
          <h1 className="text-2xl font-bold text-gray-900">
            {isEdit ? 'Edit SOAP Stub' : 'Create SOAP Stub'}
          </h1>
          <p className="mt-1 text-sm text-gray-600">
            Create a SOAP service stub with XML request/response matching
          </p>
        </div>

        <form onSubmit={handleSubmit} className="p-6">
          {formError && (
            <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-md">
              <p className="text-red-600">{formError}</p>
            </div>
          )}

          {/* Basic Information */}
          <div className="mb-8">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Basic Information</h2>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
                  Name *
                </label>
                <input
                  type="text"
                  id="name"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                />
              </div>

              <div>
                <label htmlFor="status" className="block text-sm font-medium text-gray-700 mb-1">
                  Status
                </label>
                <select
                  id="status"
                  name="status"
                  value={formData.status}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="ACTIVE">Active</option>
                  <option value="INACTIVE">Inactive</option>
                </select>
              </div>
            </div>

            <div className="mt-4">
              <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
                Description
              </label>
              <textarea
                id="description"
                name="description"
                value={formData.description}
                onChange={handleInputChange}
                rows={3}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            {/* Tags */}
            <div className="mt-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">Tags</label>
              <div className="flex flex-wrap gap-2 mb-2">
                {formData.tags.map(tag => (
                  <span key={tag} className="px-2 py-1 bg-blue-100 text-blue-800 text-sm rounded-full flex items-center">
                    {tag}
                    <button
                      type="button"
                      onClick={() => removeTag(tag)}
                      className="ml-1 text-blue-600 hover:text-blue-800"
                    >
                      ×
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
                  placeholder="Add a tag..."
                  className="flex-1 px-3 py-2 border border-gray-300 rounded-l-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <button
                  type="button"
                  onClick={addTag}
                  className="px-4 py-2 bg-blue-500 text-white rounded-r-md hover:bg-blue-600"
                >
                  Add
                </button>
              </div>
            </div>

            <div className="mt-4">
              <label className="flex items-center">
                <input
                  type="checkbox"
                  name="behindProxy"
                  checked={formData.behindProxy}
                  onChange={handleInputChange}
                  className="mr-2"
                />
                <span className="text-sm text-gray-700">Behind Proxy</span>
              </label>
            </div>
          </div>

          {/* SOAP Configuration */}
          <div className="mb-8">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">SOAP Configuration</h2>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label htmlFor="url" className="block text-sm font-medium text-gray-700 mb-1">
                  SOAP Endpoint URL *
                </label>
                <input
                  type="text"
                  id="url"
                  name="url"
                  value={formData.url}
                  onChange={handleInputChange}
                  placeholder="/soap/service"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                />
                <p className="text-xs text-gray-500 mt-1">The URL path for the SOAP service</p>
              </div>

              <div>
                <label htmlFor="soapAction" className="block text-sm font-medium text-gray-700 mb-1">
                  SOAPAction Header
                </label>
                <input
                  type="text"
                  id="soapAction"
                  name="soapAction"
                  value={formData.soapAction}
                  onChange={handleInputChange}
                  placeholder="urn:example:operation"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <p className="text-xs text-gray-500 mt-1">Optional SOAPAction header value</p>
              </div>
            </div>
          </div>

          {/* Request/Response Sections */}
          <div className="mb-8">
            <div className="flex border-b border-gray-200 mb-4">
              <button
                type="button"
                onClick={() => setActiveSection('request')}
                className={`px-4 py-2 font-medium text-sm ${
                  activeSection === 'request'
                    ? 'text-blue-600 border-b-2 border-blue-600'
                    : 'text-gray-500 hover:text-gray-700'
                }`}
              >
                Request Matching
              </button>
              <button
                type="button"
                onClick={() => setActiveSection('response')}
                className={`px-4 py-2 font-medium text-sm ${
                  activeSection === 'response'
                    ? 'text-blue-600 border-b-2 border-blue-600'
                    : 'text-gray-500 hover:text-gray-700'
                }`}
              >
                Response Configuration
              </button>
            </div>

            {activeSection === 'request' && (
              <div className="space-y-4">
                <div>
                  <label htmlFor="requestBodyMatchType" className="block text-sm font-medium text-gray-700 mb-1">
                    Body Match Type
                  </label>
                  <select
                    id="requestBodyMatchType"
                    name="requestBodyMatchType"
                    value={formData.requestBodyMatchType}
                    onChange={handleInputChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="contains">Contains</option>
                    <option value="equals">Equals</option>
                    <option value="matches">Regex Match</option>
                  </select>
                </div>

                <div>
                  <label htmlFor="requestBody" className="block text-sm font-medium text-gray-700 mb-1">
                    XML Request Body Pattern
                  </label>
                  <textarea
                    id="requestBody"
                    name="requestBody"
                    value={formData.requestBody}
                    onChange={handleInputChange}
                    rows={8}
                    placeholder="<soap:Envelope>&#10;  <soap:Body>&#10;    <!-- XML content to match -->&#10;  </soap:Body>&#10;</soap:Envelope>"
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono text-sm"
                  />
                  <p className="text-xs text-gray-500 mt-1">XML content to match in the request body</p>
                </div>
              </div>
            )}

            {activeSection === 'response' && (
              <div className="space-y-4">
                <div className="flex space-x-4 mb-4">
                  <label className="flex items-center">
                    <input
                      type="radio"
                      name="responseType"
                      value="static"
                      checked={responseType === 'static'}
                      onChange={(e) => setResponseType(e.target.value as 'static' | 'callback')}
                      className="mr-2"
                    />
                    <span className="text-sm text-gray-700">Static Response</span>
                  </label>
                  <label className="flex items-center">
                    <input
                      type="radio"
                      name="responseType"
                      value="callback"
                      checked={responseType === 'callback'}
                      onChange={(e) => setResponseType(e.target.value as 'static' | 'callback')}
                      className="mr-2"
                    />
                    <span className="text-sm text-gray-700">Webhook Response</span>
                  </label>
                </div>

                {responseType === 'static' && (
                  <>
                    <div>
                      <label htmlFor="responseStatus" className="block text-sm font-medium text-gray-700 mb-1">
                        Response Status Code
                      </label>
                      <input
                        type="number"
                        id="responseStatus"
                        name="responseStatus"
                        value={formData.responseStatus}
                        onChange={handleInputChange}
                        min="100"
                        max="599"
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>

                    <div>
                      <label htmlFor="responseBody" className="block text-sm font-medium text-gray-700 mb-1">
                        XML Response Body
                      </label>
                      <textarea
                        id="responseBody"
                        name="responseBody"
                        value={formData.responseBody}
                        onChange={handleInputChange}
                        rows={8}
                        placeholder="<soap:Envelope>&#10;  <soap:Body>&#10;    <!-- XML response content -->&#10;  </soap:Body>&#10;</soap:Envelope>"
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono text-sm"
                      />
                      <p className="text-xs text-gray-500 mt-1">XML content to return as response</p>
                    </div>
                  </>
                )}

                {responseType === 'callback' && (
                  <div>
                    <label htmlFor="callbackUrl" className="block text-sm font-medium text-gray-700 mb-1">
                      Webhook URL
                    </label>
                    <input
                      type="url"
                      id="callbackUrl"
                      name="callbackUrl"
                      value={formData.callbackUrl}
                      onChange={handleInputChange}
                      placeholder="http://localhost:8080/test-webhook/soap"
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                    <p className="text-xs text-gray-500 mt-1">URL to call for dynamic response generation</p>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Form Actions */}
          <div className="flex justify-between pt-6 border-t border-gray-200">
            <Link
              to="/soap/stubs"
              className="px-4 py-2 text-gray-600 bg-gray-100 rounded-md hover:bg-gray-200 transition-colors"
            >
              Cancel
            </Link>
            <button
              type="submit"
              disabled={isSubmitting || isCreating || isUpdating}
              className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {isSubmitting || isCreating || isUpdating ? 'Saving...' : (isEdit ? 'Update Stub' : 'Create Stub')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default SoapStubForm; 