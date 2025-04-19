import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { 
  useGetFileStubQuery,
  useCreateFileStubMutation,
  useUpdateFileStubMutation,
  FileEntry
} from '../../../api/fileApi';

type ContentSourceType = 'static' | 'webhook';

const FileStubForm: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const isEditing = Boolean(id);
  const navigate = useNavigate();
  
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    filePath: '',
    contentType: '',
    content: '',
    webhookUrl: '',
    cronExpression: '',
    status: true,
    files: [] as FileEntry[]
  });
  
  // Track content source type for primary file and additional files
  const [primaryContentSource, setPrimaryContentSource] = useState<ContentSourceType>('static');
  const [additionalContentSources, setAdditionalContentSources] = useState<ContentSourceType[]>([]);
  
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  
  const { data: stub, isLoading: isLoadingStub } = useGetFileStubQuery(id ?? '', {
    skip: !isEditing
  });
  
  const [createFileStub, { isLoading: isCreating }] = useCreateFileStubMutation();
  const [updateFileStub, { isLoading: isUpdating }] = useUpdateFileStubMutation();
  
  const isLoading = isLoadingStub || isCreating || isUpdating;
  
  useEffect(() => {
    if (stub) {
      setFormData({
        name: stub.name,
        description: stub.description || '',
        filePath: stub.filePath || '',
        contentType: stub.contentType || '',
        content: stub.content || '',
        webhookUrl: stub.webhookUrl || '',
        cronExpression: stub.cronExpression || '',
        status: stub.status,
        files: stub.files || []
      });
      
      // Set the appropriate content source based on which field has data
      setPrimaryContentSource(stub.webhookUrl ? 'webhook' : 'static');
      
      // Set content sources for additional files
      if (stub.files && stub.files.length > 0) {
        setAdditionalContentSources(
          stub.files.map(file => file.webhookUrl ? 'webhook' : 'static')
        );
      }
    }
  }, [stub]);
  
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' 
        ? (e.target as HTMLInputElement).checked 
        : name === 'status' 
          ? value === 'true' 
          : value
    }));
    
    // Clear validation error when field is modified
    if (validationErrors[name]) {
      setValidationErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  const handleFileInputChange = (index: number, e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    
    setFormData(prev => {
      const updatedFiles = [...prev.files];
      updatedFiles[index] = {
        ...updatedFiles[index],
        [name]: value
      };
      
      return {
        ...prev,
        files: updatedFiles
      };
    });
  };
  
  const addFile = () => {
    setFormData(prev => ({
      ...prev,
      files: [...prev.files, { filename: '', content: '', contentType: '', webhookUrl: '' }]
    }));
    
    // Set default content source for the new file
    setAdditionalContentSources(prev => [...prev, 'static']);
  };
  
  const removeFile = (index: number) => {
    setFormData(prev => ({
      ...prev,
      files: prev.files.filter((_, i) => i !== index)
    }));
    
    // Remove content source for the deleted file
    setAdditionalContentSources(prev => prev.filter((_, i) => i !== index));
  };
  
  const handleContentSourceChange = (source: ContentSourceType) => {
    setPrimaryContentSource(source);
    
    // Clear the non-selected field
    if (source === 'static') {
      setFormData(prev => ({ ...prev, webhookUrl: '' }));
    } else {
      setFormData(prev => ({ ...prev, content: '' }));
    }
    
    // Clear any validation errors
    setValidationErrors(prev => ({
      ...prev,
      content: '',
      webhookUrl: ''
    }));
  };
  
  const handleAdditionalContentSourceChange = (index: number, source: ContentSourceType) => {
    setAdditionalContentSources(prev => {
      const updated = [...prev];
      updated[index] = source;
      return updated;
    });
    
    // Clear the non-selected field
    setFormData(prev => {
      const updatedFiles = [...prev.files];
      updatedFiles[index] = {
        ...updatedFiles[index],
        ...(source === 'static' ? { webhookUrl: '' } : { content: '' })
      };
      return { ...prev, files: updatedFiles };
    });
    
    // Clear any validation errors
    setValidationErrors(prev => ({
      ...prev,
      [`file-${index}-content`]: '',
      [`file-${index}-webhookUrl`]: ''
    }));
  };
  
  const validateForm = () => {
    const errors: Record<string, string> = {};
    
    if (!formData.name.trim()) {
      errors.name = 'Name is required';
    }
    
    if (!formData.filePath.trim()) {
      errors.filePath = 'File path is required';
    }

    // Validate cron expression if provided
    const cronPattern = /^(\*|([0-9]|1[0-9]|2[0-9]|3[0-9]|4[0-9]|5[0-9])|\*\/([0-9]|1[0-9]|2[0-9]|3[0-9]|4[0-9]|5[0-9])) (\*|([0-9]|1[0-9]|2[0-3])|\*\/([0-9]|1[0-9]|2[0-3])) (\*|([1-9]|1[0-9]|2[0-9]|3[0-1])|\*\/([1-9]|1[0-9]|2[0-9]|3[0-1])) (\*|([1-9]|1[0-2])|\*\/([1-9]|1[0-2])) (\*|([0-6])|\*\/([0-6]))$/;
    
    if (formData.cronExpression && !cronPattern.test(formData.cronExpression.trim())) {
      errors.cronExpression = 'Invalid cron expression format';
    }
    
    // Validate webhook URL format if provided
    if (primaryContentSource === 'webhook' && formData.webhookUrl && !isValidUrl(formData.webhookUrl)) {
      errors.webhookUrl = 'Please enter a valid URL';
    }
    
    // Validate files array
    formData.files.forEach((file, index) => {
      if (!file.filename.trim()) {
        errors[`file-${index}-filename`] = 'Filename is required';
      }
      
      // Validate webhook URL format if provided
      if (additionalContentSources[index] === 'webhook' && file.webhookUrl && !isValidUrl(file.webhookUrl)) {
        errors[`file-${index}-webhookUrl`] = 'Please enter a valid URL';
      }
    });
    
    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };
  
  const isValidUrl = (url: string): boolean => {
    try {
      new URL(url);
      return true;
    } catch (e) {
      return false;
    }
  };
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }
    
    try {
      if (isEditing && id) {
        await updateFileStub({
          id,
          ...formData
        });
      } else {
        await createFileStub(formData);
      }
      navigate('/file');
    } catch (error) {
      console.error('Failed to save file stub:', error);
    }
  };
  
  return (
    <div className="container mx-auto px-4 py-6">
      <div className="flex items-center mb-6">
        <button
          onClick={() => navigate('/file')}
          className="mr-4 p-2 rounded-full hover:bg-gray-100"
        >
          <svg className="h-5 w-5 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
        </button>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            {isEditing ? 'Edit File Stub' : 'Create File Stub'}
          </h1>
        </div>
      </div>
      
      {isLoadingStub ? (
        <div className="flex justify-center items-center h-64">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600"></div>
        </div>
      ) : (
        <div className="bg-white shadow-md rounded-lg p-6">
          <form onSubmit={handleSubmit}>
            <div className="mb-4">
              <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
                Name <span className="text-red-500">*</span>
              </label>
              <input
                id="name"
                name="name"
                type="text"
                value={formData.name}
                onChange={handleInputChange}
                className={`w-full px-3 py-2 border ${validationErrors.name ? 'border-red-500' : 'border-gray-300'} rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500`}
                required
              />
              {validationErrors.name && (
                <p className="mt-1 text-sm text-red-600">{validationErrors.name}</p>
              )}
            </div>
            
            <div className="mb-4">
              <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
                Description
              </label>
              <textarea
                id="description"
                name="description"
                value={formData.description}
                onChange={handleInputChange}
                rows={2}
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
              ></textarea>
            </div>
            
            <div className="mb-4">
              <label htmlFor="filePath" className="block text-sm font-medium text-gray-700 mb-1">
                Base Directory Path <span className="text-red-500">*</span>
              </label>
              <input
                id="filePath"
                name="filePath"
                type="text"
                value={formData.filePath}
                onChange={handleInputChange}
                className={`w-full px-3 py-2 border ${validationErrors.filePath ? 'border-red-500' : 'border-gray-300'} rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500`}
                required
              />
              {validationErrors.filePath && (
                <p className="mt-1 text-sm text-red-600">{validationErrors.filePath}</p>
              )}
              <p className="mt-1 text-xs text-gray-500">The base directory where files will be generated</p>
            </div>

            <div className="mb-4">
              <label htmlFor="cronExpression" className="block text-sm font-medium text-gray-700 mb-1">
                Cron Expression
              </label>
              <input
                id="cronExpression"
                name="cronExpression"
                type="text"
                value={formData.cronExpression}
                onChange={handleInputChange}
                placeholder="e.g., */5 * * * * (every 5 minutes)"
                className={`w-full px-3 py-2 border ${validationErrors.cronExpression ? 'border-red-500' : 'border-gray-300'} rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500`}
              />
              {validationErrors.cronExpression ? (
                <p className="mt-1 text-sm text-red-600">{validationErrors.cronExpression}</p>
              ) : (
                <p className="mt-1 text-xs text-gray-500">
                  Format: minute hour day-of-month month day-of-week (e.g., "0 0 * * *" for daily at midnight)
                </p>
              )}
            </div>
            
            {/* Primary file content - for backward compatibility */}
            <div className="mb-6 p-4 border border-gray-200 rounded-md bg-gray-50">
              <h3 className="font-medium text-gray-700 mb-2">Primary File</h3>
              
              <div className="mb-4">
                <label htmlFor="contentType" className="block text-sm font-medium text-gray-700 mb-1">
                  Content Type
                </label>
                <input
                  id="contentType"
                  name="contentType"
                  type="text"
                  value={formData.contentType}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                  placeholder="e.g., text/plain, application/json, application/xml"
                />
              </div>
              
              {/* Tabs for selecting content source */}
              <div className="mb-4">
                <div className="border-b border-gray-200 mb-4">
                  <nav className="-mb-px flex" aria-label="Tabs">
                    <button
                      type="button"
                      onClick={() => handleContentSourceChange('static')}
                      className={`${
                        primaryContentSource === 'static'
                          ? 'border-primary-500 text-primary-600'
                          : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                      } w-1/2 py-2 px-1 text-center border-b-2 font-medium text-sm focus:outline-none`}
                    >
                      Static Content
                    </button>
                    <button
                      type="button"
                      onClick={() => handleContentSourceChange('webhook')}
                      className={`${
                        primaryContentSource === 'webhook'
                          ? 'border-primary-500 text-primary-600'
                          : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                      } w-1/2 py-2 px-1 text-center border-b-2 font-medium text-sm focus:outline-none`}
                    >
                      Webhook URL
                    </button>
                  </nav>
                </div>

                {/* Content based on selected tab */}
                {primaryContentSource === 'static' ? (
                  <div>
                    <label htmlFor="content" className="block text-sm font-medium text-gray-700 mb-1">
                      Static Content
                    </label>
                    <textarea
                      id="content"
                      name="content"
                      value={formData.content}
                      onChange={handleInputChange}
                      rows={8}
                      className={`w-full px-3 py-2 border ${validationErrors.content ? 'border-red-500' : 'border-gray-300'} rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500`}
                      placeholder="Enter file content here"
                    ></textarea>
                    {validationErrors.content && (
                      <p className="mt-1 text-sm text-red-600">{validationErrors.content}</p>
                    )}
                  </div>
                ) : (
                  <div>
                    <label htmlFor="webhookUrl" className="block text-sm font-medium text-gray-700 mb-1">
                      Webhook URL
                    </label>
                    <input
                      id="webhookUrl"
                      name="webhookUrl"
                      type="text"
                      value={formData.webhookUrl}
                      onChange={handleInputChange}
                      className={`w-full px-3 py-2 border ${validationErrors.webhookUrl ? 'border-red-500' : 'border-gray-300'} rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500`}
                      placeholder="https://example.com/api/data"
                    />
                    {validationErrors.webhookUrl && (
                      <p className="mt-1 text-sm text-red-600">{validationErrors.webhookUrl}</p>
                    )}
                    <p className="mt-1 text-xs text-gray-500">
                      Content will be fetched from this URL when generating the file
                    </p>
                  </div>
                )}
              </div>
            </div>
            
            {/* Multiple files section */}
            <div className="mb-6">
              <div className="flex justify-between items-center mb-3">
                <h3 className="text-lg font-medium text-gray-700">Additional Files</h3>
                <button
                  type="button"
                  onClick={addFile}
                  className="inline-flex items-center px-3 py-1 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-primary-600 hover:bg-primary-700"
                >
                  <svg className="h-4 w-4 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                  </svg>
                  Add File
                </button>
              </div>
              
              {formData.files.length === 0 ? (
                <p className="text-sm text-gray-500 mb-4 italic">No additional files. Click 'Add File' to add more files.</p>
              ) : (
                formData.files.map((file, index) => (
                  <div key={index} className="mb-6 p-4 border border-gray-200 rounded-md bg-gray-50 relative">
                    <button
                      type="button"
                      onClick={() => removeFile(index)}
                      className="absolute top-2 right-2 text-red-500 hover:text-red-700"
                    >
                      <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                    
                    <h4 className="font-medium text-gray-700 mb-3">File #{index + 1}</h4>
                    
                    <div className="mb-3">
                      <label 
                        htmlFor={`file-${index}-filename`} 
                        className="block text-sm font-medium text-gray-700 mb-1"
                      >
                        Filename <span className="text-red-500">*</span>
                      </label>
                      <input
                        id={`file-${index}-filename`}
                        name="filename"
                        type="text"
                        value={file.filename}
                        onChange={(e) => handleFileInputChange(index, e)}
                        className={`w-full px-3 py-2 border ${
                          validationErrors[`file-${index}-filename`] 
                            ? 'border-red-500' 
                            : 'border-gray-300'
                        } rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500`}
                        required
                      />
                      {validationErrors[`file-${index}-filename`] && (
                        <p className="mt-1 text-sm text-red-600">{validationErrors[`file-${index}-filename`]}</p>
                      )}
                    </div>
                    
                    <div className="mb-3">
                      <label 
                        htmlFor={`file-${index}-contentType`} 
                        className="block text-sm font-medium text-gray-700 mb-1"
                      >
                        Content Type
                      </label>
                      <input
                        id={`file-${index}-contentType`}
                        name="contentType"
                        type="text"
                        value={file.contentType || ''}
                        onChange={(e) => handleFileInputChange(index, e)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                        placeholder="e.g., text/plain, application/json, application/xml"
                      />
                    </div>
                    
                    {/* Tabs for selecting content source for this file */}
                    <div className="border-b border-gray-200 mb-4">
                      <nav className="-mb-px flex" aria-label="Tabs">
                        <button
                          type="button"
                          onClick={() => handleAdditionalContentSourceChange(index, 'static')}
                          className={`${
                            additionalContentSources[index] === 'static'
                              ? 'border-primary-500 text-primary-600'
                              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                          } w-1/2 py-2 px-1 text-center border-b-2 font-medium text-sm focus:outline-none`}
                        >
                          Static Content
                        </button>
                        <button
                          type="button"
                          onClick={() => handleAdditionalContentSourceChange(index, 'webhook')}
                          className={`${
                            additionalContentSources[index] === 'webhook'
                              ? 'border-primary-500 text-primary-600'
                              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                          } w-1/2 py-2 px-1 text-center border-b-2 font-medium text-sm focus:outline-none`}
                        >
                          Webhook URL
                        </button>
                      </nav>
                    </div>
                    
                    {/* Content based on selected tab for this file */}
                    {additionalContentSources[index] === 'static' ? (
                      <div>
                        <label 
                          htmlFor={`file-${index}-content`} 
                          className="block text-sm font-medium text-gray-700 mb-1"
                        >
                          Static Content
                        </label>
                        <textarea
                          id={`file-${index}-content`}
                          name="content"
                          value={file.content || ''}
                          onChange={(e) => handleFileInputChange(index, e)}
                          rows={5}
                          className={`w-full px-3 py-2 border ${
                            validationErrors[`file-${index}-content`] 
                              ? 'border-red-500' 
                              : 'border-gray-300'
                          } rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500`}
                          placeholder="Enter file content here"
                        ></textarea>
                        {validationErrors[`file-${index}-content`] && (
                          <p className="mt-1 text-sm text-red-600">{validationErrors[`file-${index}-content`]}</p>
                        )}
                      </div>
                    ) : (
                      <div>
                        <label 
                          htmlFor={`file-${index}-webhookUrl`} 
                          className="block text-sm font-medium text-gray-700 mb-1"
                        >
                          Webhook URL
                        </label>
                        <input
                          id={`file-${index}-webhookUrl`}
                          name="webhookUrl"
                          type="text"
                          value={file.webhookUrl || ''}
                          onChange={(e) => handleFileInputChange(index, e)}
                          className={`w-full px-3 py-2 border ${
                            validationErrors[`file-${index}-webhookUrl`] 
                              ? 'border-red-500' 
                              : 'border-gray-300'
                          } rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500`}
                          placeholder="https://example.com/api/data"
                        />
                        {validationErrors[`file-${index}-webhookUrl`] && (
                          <p className="mt-1 text-sm text-red-600">{validationErrors[`file-${index}-webhookUrl`]}</p>
                        )}
                        <p className="mt-1 text-xs text-gray-500">
                          Content will be fetched from this URL when generating the file
                        </p>
                      </div>
                    )}
                  </div>
                ))
              )}
            </div>
            
            <div className="mb-6">
              <label htmlFor="status" className="block text-sm font-medium text-gray-700 mb-1">
                Status
              </label>
              <select
                id="status"
                name="status"
                value={formData.status ? "true" : "false"}
                onChange={handleInputChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
              >
                <option value="true">Enabled</option>
                <option value="false">Disabled</option>
              </select>
            </div>
            
            <div className="flex justify-end">
              <button
                type="submit"
                disabled={isLoading}
                className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isLoading ? (
                  <>
                    <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Processing...
                  </>
                ) : isEditing ? 'Update' : 'Create'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};

export default FileStubForm; 