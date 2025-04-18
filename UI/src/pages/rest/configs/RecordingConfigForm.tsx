import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

interface RecordingConfigFormProps {
  isEdit?: boolean;
}

const RecordingConfigForm: React.FC<RecordingConfigFormProps> = ({ isEdit = false }) => {
  const navigate = useNavigate();
  
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    urlPattern: '',
    useHttps: false,
    certificateFile: null as File | null,
    certificatePassword: ''
  });

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value, type } = e.target;
    setFormData({
      ...formData,
      [name]: type === 'checkbox' ? (e.target as HTMLInputElement).checked : value
    });
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] || null;
    setFormData({
      ...formData,
      certificateFile: file
    });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    // In a real app, we would create a FormData object and make an API call here
    const data = new FormData();
    data.append('name', formData.name);
    data.append('description', formData.description);
    data.append('urlPattern', formData.urlPattern);
    data.append('useHttps', String(formData.useHttps));
    
    if (formData.certificateFile) {
      data.append('certificateFile', formData.certificateFile);
      data.append('certificatePassword', formData.certificatePassword);
    }
    
    console.log('Form submitted:', Object.fromEntries(data));
    
    // Redirect back to the REST dashboard
    navigate('/rest');
  };

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
          <h2 className="text-xl font-semibold text-gray-800">
            {isEdit ? 'Edit Recording Configuration' : 'Create Recording Configuration'}
          </h2>
        </div>

        <form onSubmit={handleSubmit} className="p-6">
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
            </div>
          </div>

          <div className="mb-8">
            <h3 className="text-lg font-medium text-gray-700 mb-4">URL Configuration</h3>
            <div className="mb-4">
              <label htmlFor="urlPattern" className="block text-sm font-medium text-gray-700 mb-1">
                URL Pattern <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                id="urlPattern"
                name="urlPattern"
                value={formData.urlPattern}
                onChange={handleInputChange}
                placeholder="e.g., /api/users/**"
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                required
              />
              <p className="mt-1 text-xs text-gray-500">
                Specify URL patterns that should be recorded. Use ** as a wildcard.
              </p>
            </div>

            <div className="flex items-center mb-4">
              <input
                type="checkbox"
                id="useHttps"
                name="useHttps"
                checked={formData.useHttps}
                onChange={handleInputChange}
                className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
              />
              <label htmlFor="useHttps" className="ml-2 block text-sm text-gray-700">
                Use HTTPS for this configuration
              </label>
            </div>
          </div>

          {formData.useHttps && (
            <div className="mb-8">
              <h3 className="text-lg font-medium text-gray-700 mb-4">SSL Certificate</h3>
              <div className="mb-4">
                <label htmlFor="certificateFile" className="block text-sm font-medium text-gray-700 mb-1">
                  Certificate File
                </label>
                <input
                  type="file"
                  id="certificateFile"
                  name="certificateFile"
                  onChange={handleFileChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                />
                <p className="mt-1 text-xs text-gray-500">
                  Upload a PKCS#12 (.p12 or .pfx) certificate file
                </p>
              </div>

              <div className="mb-4">
                <label htmlFor="certificatePassword" className="block text-sm font-medium text-gray-700 mb-1">
                  Certificate Password
                </label>
                <input
                  type="password"
                  id="certificatePassword"
                  name="certificatePassword"
                  value={formData.certificatePassword}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                />
              </div>
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
              className="bg-primary-600 py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
            >
              {isEdit ? 'Update Configuration' : 'Create Configuration'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RecordingConfigForm; 