import React, { useState } from 'react';
import { 
  usePublishMessageMutation,
  MessageHeader
} from '../../api/ibmMqApi';

interface FormData {
  destinationType: string;
  destinationName: string;
  message: string;
  headers: MessageHeader[];
}

/**
 * Component for publishing messages on-demand to IBM MQ queues and topics
 */
const IBMMQOnDemandPublish: React.FC = () => {
  const [formData, setFormData] = useState<FormData>({
    destinationType: 'queue',
    destinationName: '',
    message: '',
    headers: []
  });

  const [publishMessage, { isLoading }] = usePublishMessageMutation();
  const [publishResult, setPublishResult] = useState<{ success: boolean; message: string } | null>(null);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleAddHeader = () => {
    setFormData(prev => ({
      ...prev,
      headers: [...prev.headers, { name: '', value: '', type: 'string' }]
    }));
  };

  const handleRemoveHeader = (index: number) => {
    setFormData(prev => ({
      ...prev,
      headers: prev.headers.filter((_, i) => i !== index)
    }));
  };

  const handleHeaderChange = (index: number, field: keyof MessageHeader, value: string) => {
    setFormData(prev => ({
      ...prev,
      headers: prev.headers.map((header, i) => 
        i === index ? { ...header, [field]: value } : header
      )
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPublishResult(null);

    if (!formData.destinationName.trim() || !formData.message.trim()) {
      setPublishResult({
        success: false,
        message: 'Destination Name and Message are required fields.'
      });
      return;
    }

    try {
      const result = await publishMessage({
        destinationType: formData.destinationType,
        destinationName: formData.destinationName.trim(),
        message: formData.message,
        headers: formData.headers.filter(h => h.name.trim() && h.value.trim())
      }).unwrap();

      setPublishResult(result);
      
      // Reset form on success
      if (result.success) {
        setFormData({
          destinationType: 'queue',
          destinationName: '',
          message: '',
          headers: []
        });
      }
    } catch (error: any) {
      setPublishResult({
        success: false,
        message: error?.data?.message || 'Failed to publish message'
      });
    }
  };

  const handleClear = () => {
    setFormData({
      destinationType: 'queue',
      destinationName: '',
      message: '',
      headers: []
    });
    setPublishResult(null);
  };

  return (
    <div className="max-w-4xl mx-auto">
      <div className="bg-white shadow rounded-lg p-6">
        <h2 className="text-2xl font-bold text-gray-900 mb-6">Publish Message to IBM MQ Queue or Topic</h2>

        {publishResult && (
          <div className={`mb-6 p-4 rounded-md ${
            publishResult.success 
              ? 'bg-green-50 border border-green-200 text-green-800' 
              : 'bg-red-50 border border-red-200 text-red-800'
          }`}>
            <div className="flex">
              <div className="flex-shrink-0">
                {publishResult.success ? (
                  <svg className="h-5 w-5 text-green-400" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                  </svg>
                ) : (
                  <svg className="h-5 w-5 text-red-400" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                  </svg>
                )}
              </div>
              <div className="ml-3">
                <p className="text-sm font-medium">
                  {publishResult.success ? 'Success!' : 'Error'}
                </p>
                <p className="text-sm mt-1">{publishResult.message}</p>
              </div>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Destination Configuration */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label htmlFor="destinationType" className="block text-sm font-medium text-gray-700 mb-2">
                Destination Type *
              </label>
              <select
                value={formData.destinationType}
                onChange={(e) => handleInputChange({ target: { name: 'destinationType', value: e.target.value } })}
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                required
              >
                <option value="queue">Queue</option>
                <option value="topic">Topic</option>
              </select>
            </div>

            <div>
              <label htmlFor="destinationName" className="block text-sm font-medium text-gray-700 mb-2">
                Destination Name *
              </label>
              <input
                type="text"
                id="destinationName"
                name="destinationName"
                value={formData.destinationName}
                onChange={handleInputChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                placeholder="e.g., REQUEST.QUEUE or REQUEST.TOPIC"
                required
              />
            </div>
          </div>

          {/* Message Content */}
          <div>
            <label htmlFor="message" className="block text-sm font-medium text-gray-700 mb-2">
              Message Content *
            </label>
            <textarea
              id="message"
              name="message"
              value={formData.message}
              onChange={handleInputChange}
              rows={8}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              placeholder="Enter your message content here..."
              required
            />
          </div>

          {/* Headers Section */}
          <div>
            <div className="flex justify-between items-center mb-4">
              <label className="block text-sm font-medium text-gray-700">
                Message Headers
              </label>
              <button
                type="button"
                onClick={handleAddHeader}
                className="bg-blue-600 hover:bg-blue-700 text-white text-sm px-3 py-1 rounded"
              >
                Add Header
              </button>
            </div>

            {formData.headers.map((header, index) => (
              <div key={index} className="grid grid-cols-1 md:grid-cols-3 gap-3 mb-3 p-4 border border-gray-200 rounded-md">
                <div>
                  <input
                    type="text"
                    placeholder="Header Name"
                    value={header.name}
                    onChange={(e) => handleHeaderChange(index, 'name', e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  />
                </div>
                <div>
                  <input
                    type="text"
                    placeholder="Header Value"
                    value={header.value}
                    onChange={(e) => handleHeaderChange(index, 'value', e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  />
                </div>
                <div className="flex items-center space-x-2">
                  <select
                    value={header.type}
                    onChange={(e) => handleHeaderChange(index, 'type', e.target.value)}
                    className="flex-1 px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  >
                    <option value="string">String</option>
                    <option value="integer">Integer</option>
                    <option value="boolean">Boolean</option>
                  </select>
                  <button
                    type="button"
                    onClick={() => handleRemoveHeader(index)}
                    className="bg-red-600 hover:bg-red-700 text-white px-2 py-1 rounded text-sm"
                  >
                    Remove
                  </button>
                </div>
              </div>
            ))}
          </div>

          {/* Action Buttons */}
          <div className="flex justify-end space-x-3 pt-6 border-t border-gray-200">
            <button
              type="button"
              onClick={handleClear}
              className="bg-gray-300 hover:bg-gray-400 text-gray-700 px-4 py-2 rounded-md"
            >
              Clear
            </button>
            <button
              type="submit"
              disabled={isLoading}
              className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-md disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
            >
              {isLoading && (
                <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
              )}
              {isLoading ? 'Publishing...' : 'Publish Message'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default IBMMQOnDemandPublish; 