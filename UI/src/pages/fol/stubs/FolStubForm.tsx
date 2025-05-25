import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  useCreateFolStubMutation, 
  useGetFolStubQuery, 
  useUpdateFolStubMutation,
  FolEntry
} from '../../../api/folApi';

interface FolStubFormProps {
  isEdit?: boolean;
  stubId?: string;
}

const FolStubForm: React.FC<FolStubFormProps> = ({ isEdit = false, stubId = '' }) => {
  const navigate = useNavigate();
  
  const [createFolStub, { isLoading: isCreating }] = useCreateFolStubMutation();
  const [updateFolStub, { isLoading: isUpdating }] = useUpdateFolStubMutation();
  
  const { data: existingStub, isLoading: isLoadingStub } = useGetFolStubQuery(stubId, {
    skip: !isEdit || !stubId,
  });

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    filePath: '',
    content: '',
    contentType: 'text/plain',
    webhookUrl: '',
    cronExpression: '',
    status: true,
    files: [] as FolEntry[]
  });

  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [additionalFile, setAdditionalFile] = useState<FolEntry>({
    filename: '',
    content: '',
    contentType: 'text/plain',
    webhookUrl: ''
  });

  const [showAddFileForm, setShowAddFileForm] = useState(false);

  useEffect(() => {
    if (isEdit && existingStub) {
      setFormData({
        name: existingStub.name || '',
        description: existingStub.description || '',
        filePath: existingStub.filePath || '',
        content: existingStub.content || '',
        contentType: existingStub.contentType || 'text/plain',
        webhookUrl: existingStub.webhookUrl || '',
        cronExpression: existingStub.cronExpression || '',
        status: existingStub.status,
        files: existingStub.files || []
      });
    }
  }, [isEdit, existingStub]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    
    if (type === 'checkbox') {
      const checked = (e.target as HTMLInputElement).checked;
      setFormData({
        ...formData,
        [name]: checked,
      });
    } else {
      setFormData({
        ...formData,
        [name]: value,
      });
    }
  };

  const handleAdditionalFileChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setAdditionalFile({
      ...additionalFile,
      [name]: value
    });
  };

  const addAdditionalFile = () => {
    if (!additionalFile.filename.trim()) {
      setFormErrors({
        ...formErrors,
        additionalFile: 'Filename is required for additional file'
      });
      return;
    }

    if (!additionalFile.content && !additionalFile.webhookUrl) {
      setFormErrors({
        ...formErrors,
        additionalFile: 'Either content or webhook URL is required'
      });
      return;
    }

    setFormData({
      ...formData,
      files: [...formData.files, { ...additionalFile }]
    });

    // Reset additional file form
    setAdditionalFile({
      filename: '',
      content: '',
      contentType: 'text/plain',
      webhookUrl: ''
    });

    // Clear error if it exists
    const newErrors = { ...formErrors };
    delete newErrors.additionalFile;
    setFormErrors(newErrors);
  };

  const removeAdditionalFile = (index: number) => {
    const updatedFiles = [...formData.files];
    updatedFiles.splice(index, 1);
    setFormData({
      ...formData,
      files: updatedFiles
    });
  };

  const validateForm = () => {
    const errors: Record<string, string> = {};
    
    if (!formData.name.trim()) {
      errors.name = 'Name is required';
    }
    
    if (!formData.filePath.trim()) {
      errors.filePath = 'File path is required';
    }
    
    if (!formData.content && !formData.webhookUrl) {
      errors.content = 'Either content or webhook URL is required';
    }

    if (formData.content && formData.webhookUrl) {
      errors.webhookUrl = 'You can provide either content or webhook URL, not both';
    }
    
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }
    
    setIsSubmitting(true);
    
    try {
      if (isEdit && stubId) {
        await updateFolStub({
          id: stubId,
          ...formData,
        }).unwrap();
      } else {
        await createFolStub(formData).unwrap();
      }
      
      navigate('/fol');
    } catch (error) {
      console.error('Failed to save stub:', error);
      setFormErrors({
        submit: 'Failed to save the stub. Please try again.',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isEdit && isLoadingStub) {
    return (
      <div className="flex justify-center items-center py-8">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 py-6">
      <div className="mb-6 flex items-center">
        <button
          onClick={() => navigate('/fol')}
          className="mr-4 p-2 rounded-full hover:bg-gray-100"
          aria-label="Go back"
        >
          <svg className="h-5 w-5 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
        </button>
        <h1 className="text-2xl font-bold text-gray-900">
          {isEdit ? 'Edit File Stub' : 'Create File Stub'}
        </h1>
      </div>

      <form onSubmit={handleSubmit} className="bg-white shadow-md rounded-lg overflow-hidden">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-lg font-medium text-gray-900 mb-4">Basic Information</h2>
          
          <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1" htmlFor="name">
                Name <span className="text-red-500">*</span>
              </label>
              <input
                className={`shadow-sm appearance-none border ${formErrors.name ? 'border-red-500' : 'border-gray-300'} rounded-md w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:ring-primary-500 focus:border-primary-500`}
                id="name"
                type="text"
                name="name"
                value={formData.name}
                onChange={handleInputChange}
                placeholder="Enter stub name"
              />
              {formErrors.name && <p className="mt-1 text-sm text-red-500">{formErrors.name}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1" htmlFor="filePath">
                File Path <span className="text-red-500">*</span>
              </label>
              <input
                className={`shadow-sm appearance-none border ${formErrors.filePath ? 'border-red-500' : 'border-gray-300'} rounded-md w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:ring-primary-500 focus:border-primary-500`}
                id="filePath"
                type="text"
                name="filePath"
                value={formData.filePath}
                onChange={handleInputChange}
                placeholder="/path/to/file.txt"
              />
              {formErrors.filePath && <p className="mt-1 text-sm text-red-500">{formErrors.filePath}</p>}
            </div>

            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1" htmlFor="description">
                Description
              </label>
              <textarea
                className="shadow-sm appearance-none border border-gray-300 rounded-md w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                id="description"
                name="description"
                value={formData.description}
                onChange={handleInputChange}
                placeholder="Enter stub description"
                rows={2}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1" htmlFor="cronExpression">
                Cron Expression
              </label>
              <input
                className="shadow-sm appearance-none border border-gray-300 rounded-md w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                id="cronExpression"
                type="text"
                name="cronExpression"
                value={formData.cronExpression}
                onChange={handleInputChange}
                placeholder="0 0 * * * *"
              />
              <p className="mt-1 text-xs text-gray-500">Format: second minute hour day-of-month month day-of-week</p>
            </div>

            <div className="self-end">
              <label className="inline-flex items-center">
                <input
                  type="checkbox"
                  name="status"
                  checked={formData.status}
                  onChange={(e) => setFormData({...formData, status: e.target.checked})}
                  className="rounded border-gray-300 text-primary-600 shadow-sm focus:border-primary-300 focus:ring focus:ring-primary-200 focus:ring-opacity-50"
                />
                <span className="ml-2 text-sm text-gray-700">Active</span>
              </label>
            </div>
          </div>
        </div>

        <div className="p-6 border-b border-gray-200">
          <h2 className="text-lg font-medium text-gray-900 mb-4">Primary File Content</h2>
          
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-1" htmlFor="contentType">
              Content Type
            </label>
            <select
              className="shadow-sm appearance-none border border-gray-300 rounded-md w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:ring-primary-500 focus:border-primary-500"
              id="contentType"
              name="contentType"
              value={formData.contentType}
              onChange={handleInputChange}
            >
              <option value="text/plain">Text</option>
              <option value="application/json">JSON</option>
              <option value="application/xml">XML</option>
              <option value="text/csv">CSV</option>
              <option value="application/octet-stream">Binary</option>
            </select>
          </div>

          <div className="mb-6">
            <div className="flex justify-between items-center mb-1">
              <label className="block text-sm font-medium text-gray-700" htmlFor="content">
                Content
              </label>
              <span className="text-xs text-gray-500">
                Provide either content or webhook URL
              </span>
            </div>
            <textarea
              className={`shadow-sm appearance-none border ${formErrors.content ? 'border-red-500' : 'border-gray-300'} rounded-md w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:ring-primary-500 focus:border-primary-500`}
              id="content"
              name="content"
              value={formData.content}
              onChange={handleInputChange}
              placeholder="Enter file content"
              rows={8}
              disabled={!!formData.webhookUrl}
            />
            {formErrors.content && <p className="mt-1 text-sm text-red-500">{formErrors.content}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1" htmlFor="webhookUrl">
              Webhook URL
            </label>
            <input
              className={`shadow-sm appearance-none border ${formErrors.webhookUrl ? 'border-red-500' : 'border-gray-300'} rounded-md w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:ring-primary-500 focus:border-primary-500`}
              id="webhookUrl"
              type="text"
              name="webhookUrl"
              value={formData.webhookUrl}
              onChange={handleInputChange}
              placeholder="https://example.com/api/webhook"
              disabled={!!formData.content}
            />
            {formErrors.webhookUrl && <p className="mt-1 text-sm text-red-500">{formErrors.webhookUrl}</p>}
            <p className="mt-1 text-xs text-gray-500">
              If provided, content will be fetched from this URL when the stub is accessed
            </p>
          </div>
        </div>

        <div className="p-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">Additional Files</h2>
          
          <div className="bg-gray-50 p-4 rounded-lg border border-gray-200 mb-4">
            <div className="flex justify-between items-center mb-3">
              <h3 className="text-md font-medium text-gray-700">
                Additional Files 
                {formData.files.length > 0 && (
                  <span className="ml-2 px-2 py-0.5 text-sm bg-primary-100 text-primary-800 rounded-full">
                    {formData.files.length} file{formData.files.length !== 1 ? 's' : ''} added
                  </span>
                )}
              </h3>
            </div>
            
            {formData.files.length > 0 && (
              <div className="mb-6 overflow-hidden bg-white rounded-lg border border-gray-200">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-100">
                    <tr>
                      <th scope="col" className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Filename
                      </th>
                      <th scope="col" className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Content Type
                      </th>
                      <th scope="col" className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Source
                      </th>
                      <th scope="col" className="px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider text-right">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    {formData.files.map((file, index) => (
                      <tr key={index} className="hover:bg-gray-100">
                        <td className="px-4 py-3 whitespace-nowrap text-sm font-medium text-gray-900">
                          {file.filename}
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                          {file.contentType || 'text/plain'}
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                          {file.webhookUrl ? 'Webhook' : 'Static Content'}
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500 text-right">
                          <button
                            type="button"
                            onClick={() => removeAdditionalFile(index)}
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
            )}

            <div className="bg-white p-4 rounded-lg border border-gray-200">
              <div className="flex justify-between items-center mb-3">
                <h4 className="text-md font-medium text-gray-700">Add New File</h4>
              </div>
              
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1" htmlFor="filename">
                    Filename <span className="text-red-500">*</span>
                  </label>
                  <input
                    className={`shadow-sm appearance-none border ${formErrors.additionalFile ? 'border-red-500' : 'border-gray-300'} rounded-md w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:ring-primary-500 focus:border-primary-500`}
                    id="filename"
                    type="text"
                    name="filename"
                    value={additionalFile.filename}
                    onChange={handleAdditionalFileChange}
                    placeholder="additional_file.txt"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1" htmlFor="additional-contentType">
                    Content Type
                  </label>
                  <select
                    className="shadow-sm appearance-none border border-gray-300 rounded-md w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                    id="additional-contentType"
                    name="contentType"
                    value={additionalFile.contentType}
                    onChange={handleAdditionalFileChange}
                  >
                    <option value="text/plain">Text</option>
                    <option value="application/json">JSON</option>
                    <option value="application/xml">XML</option>
                    <option value="text/csv">CSV</option>
                    <option value="application/octet-stream">Binary</option>
                  </select>
                </div>

                <div className="md:col-span-2">
                  <div className="flex justify-between items-center mb-1">
                    <label className="block text-sm font-medium text-gray-700" htmlFor="additional-content">
                      Content
                    </label>
                    <span className="text-xs text-gray-500">
                      Provide either content or webhook URL
                    </span>
                  </div>
                  <textarea
                    className="shadow-sm appearance-none border border-gray-300 rounded-md w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                    id="additional-content"
                    name="content"
                    value={additionalFile.content}
                    onChange={handleAdditionalFileChange}
                    placeholder="Enter file content"
                    rows={4}
                    disabled={!!additionalFile.webhookUrl}
                  />
                </div>

                <div className="md:col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-1" htmlFor="additional-webhookUrl">
                    Webhook URL
                  </label>
                  <input
                    className="shadow-sm appearance-none border border-gray-300 rounded-md w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                    id="additional-webhookUrl"
                    type="text"
                    name="webhookUrl"
                    value={additionalFile.webhookUrl}
                    onChange={handleAdditionalFileChange}
                    placeholder="https://example.com/api/webhook"
                    disabled={!!additionalFile.content}
                  />
                </div>
              </div>

              {formErrors.additionalFile && (
                <p className="mt-3 text-sm text-red-500">{formErrors.additionalFile}</p>
              )}
              
              <div className="mt-4 flex justify-end">
                <button
                  type="button"
                  onClick={addAdditionalFile}
                  className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
                >
                  Add This File
                </button>
              </div>
            </div>
            
            {formData.files.length > 0 && (
              <div className="mt-4 px-4 py-3 bg-gray-100 rounded-lg text-sm text-gray-700">
                <p className="flex items-center">
                  <svg className="w-4 h-4 mr-2 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2h-1V9z" clipRule="evenodd"></path>
                  </svg>
                  You've added {formData.files.length} additional file{formData.files.length !== 1 ? 's' : ''}. You can continue adding more files as needed.
                </p>
              </div>
            )}
          </div>
        </div>

        {formErrors.submit && (
          <div className="px-6 py-3 bg-red-50 text-red-700 border-t border-red-200">
            {formErrors.submit}
          </div>
        )}

        <div className="px-6 py-4 bg-gray-50 border-t border-gray-200 flex justify-between">
          <button
            type="button"
            onClick={() => navigate('/fol')}
            className="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 disabled:opacity-50"
          >
            {isSubmitting ? 
              'Saving...' : 
              isEdit ? 'Update Stub' : 'Create Stub'
            }
          </button>
        </div>
      </form>
    </div>
  );
};

export default FolStubForm;