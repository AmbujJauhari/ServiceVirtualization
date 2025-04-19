import React, { useState, useEffect } from 'react';
import { useGetTibcoDestinationsQuery, usePublishTibcoMessageMutation } from '../../../api/tibcoApi';
import TextEditor from '../../../components/common/TextEditor';

interface MessageProperty {
  key: string;
  value: string;
}

const contentTypeOptions = [
  { value: 'text/plain', label: 'Plain Text' },
  { value: 'application/json', label: 'JSON' },
  { value: 'application/xml', label: 'XML' }
];

const TibcoPublisher: React.FC = () => {
  const [selectedDestination, setSelectedDestination] = useState('');
  const [destinationType, setDestinationType] = useState<'TOPIC' | 'QUEUE'>('TOPIC');
  const [messageText, setMessageText] = useState('');
  const [contentType, setContentType] = useState(contentTypeOptions[0].value);
  const [properties, setProperties] = useState<MessageProperty[]>([{ key: '', value: '' }]);
  const [isSuccessful, setIsSuccessful] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const { data: destinations, isLoading: isLoadingDestinations } = useGetTibcoDestinationsQuery();
  const [publishMessage, { isLoading: isPublishing }] = usePublishTibcoMessageMutation();
  
  const filteredDestinations = destinations?.filter(dest => dest.type === destinationType) || [];

  // Reset success message after 3 seconds
  useEffect(() => {
    let timer: NodeJS.Timeout;
    if (isSuccessful) {
      timer = setTimeout(() => {
        setIsSuccessful(false);
        setSuccessMessage('');
      }, 3000);
    }
    return () => {
      if (timer) clearTimeout(timer);
    };
  }, [isSuccessful]);

  const addProperty = () => {
    setProperties([...properties, { key: '', value: '' }]);
  };

  const removeProperty = (index: number) => {
    const updatedProperties = [...properties];
    updatedProperties.splice(index, 1);
    setProperties(updatedProperties.length ? updatedProperties : [{ key: '', value: '' }]);
  };

  const handlePropertyChange = (index: number, field: 'key' | 'value', value: string) => {
    const updatedProperties = [...properties];
    updatedProperties[index][field] = value;
    setProperties(updatedProperties);
  };

  const getFormattedExample = () => {
    switch (contentType) {
      case 'application/json':
        return `{\n  "message": "Hello TIBCO",\n  "timestamp": "${new Date().toISOString()}"\n}`;
      case 'application/xml':
        return `<message>\n  <content>Hello TIBCO</content>\n  <timestamp>${new Date().toISOString()}</timestamp>\n</message>`;
      default:
        return 'Hello TIBCO!';
    }
  };

  const handleDestinationTypeChange = (type: 'TOPIC' | 'QUEUE') => {
    setDestinationType(type);
    setSelectedDestination('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');
    setIsSuccessful(false);
    
    if (!selectedDestination || !messageText.trim()) {
      setErrorMessage('Please select a destination and enter a message.');
      return;
    }

    // Format properties, filtering out empty ones
    const propertyMap: Record<string, string> = {};
    properties.forEach(prop => {
      if (prop.key.trim()) {
        propertyMap[prop.key.trim()] = prop.value;
      }
    });

    try {
      await publishMessage({
        destinationType,
        destinationName: selectedDestination,
        contentType,
        message: messageText,
        properties: propertyMap
      }).unwrap();
      
      setIsSuccessful(true);
      setSuccessMessage(`Message successfully published to ${destinationType.toLowerCase()} "${selectedDestination}"`);
    } catch (error) {
      console.error('Error publishing message:', error);
      setErrorMessage(
        error instanceof Error 
          ? error.message 
          : 'Failed to publish message. Please try again.'
      );
    }
  };

  return (
    <div className="p-6">
      <h2 className="text-lg font-medium text-gray-700 mb-6">Publish Message</h2>
      
      {isSuccessful && (
        <div className="mb-4 p-3 bg-green-100 border border-green-200 text-green-700 rounded">
          {successMessage}
        </div>
      )}
      
      {errorMessage && (
        <div className="mb-4 p-3 bg-red-100 border border-red-200 text-red-700 rounded">
          {errorMessage}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div className="mb-6">
          <h3 className="text-md font-medium text-gray-700 mb-2">Destination</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Destination Type
              </label>
              <div className="flex gap-2">
                <button
                  type="button"
                  className={`flex-1 py-2 px-4 rounded ${
                    destinationType === 'TOPIC'
                      ? 'bg-primary-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                  onClick={() => handleDestinationTypeChange('TOPIC')}
                >
                  Topic
                </button>
                <button
                  type="button"
                  className={`flex-1 py-2 px-4 rounded ${
                    destinationType === 'QUEUE'
                      ? 'bg-primary-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                  onClick={() => handleDestinationTypeChange('QUEUE')}
                >
                  Queue
                </button>
              </div>
            </div>
            <div>
              <label htmlFor="destination" className="block text-sm font-medium text-gray-700 mb-1">
                Destination Name
              </label>
              <select
                id="destination"
                value={selectedDestination}
                onChange={(e) => setSelectedDestination(e.target.value)}
                className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                required
              >
                <option value="">Select a destination</option>
                {isLoadingDestinations ? (
                  <option disabled>Loading destinations...</option>
                ) : filteredDestinations.length === 0 ? (
                  <option disabled>No {destinationType.toLowerCase()}s available</option>
                ) : (
                  filteredDestinations.map((dest) => (
                    <option key={dest.name} value={dest.name}>
                      {dest.name}
                    </option>
                  ))
                )}
              </select>
            </div>
          </div>
        </div>

        <div className="mb-6">
          <h3 className="text-md font-medium text-gray-700 mb-2">Message Properties</h3>
          <div className="mb-3">
            <label htmlFor="contentType" className="block text-sm font-medium text-gray-700 mb-1">
              Content Type
            </label>
            <select
              id="contentType"
              value={contentType}
              onChange={(e) => setContentType(e.target.value)}
              className="w-full md:w-1/3 rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
            >
              {contentTypeOptions.map(option => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>
          
          <div className="mb-3">
            <div className="flex justify-between items-center mb-2">
              <label className="block text-sm font-medium text-gray-700">
                Custom Properties
              </label>
              <button
                type="button"
                onClick={addProperty}
                className="text-primary-600 hover:text-primary-800 text-sm font-medium flex items-center"
              >
                <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                </svg>
                Add Property
              </button>
            </div>

            {properties.map((prop, index) => (
              <div key={index} className="flex gap-2 mb-2">
                <input
                  type="text"
                  placeholder="Property Name"
                  value={prop.key}
                  onChange={(e) => handlePropertyChange(index, 'key', e.target.value)}
                  className="flex-1 rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                />
                <input
                  type="text"
                  placeholder="Property Value"
                  value={prop.value}
                  onChange={(e) => handlePropertyChange(index, 'value', e.target.value)}
                  className="flex-1 rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
                />
                <button
                  type="button"
                  onClick={() => removeProperty(index)}
                  className="p-2 text-gray-400 hover:text-red-600"
                  disabled={properties.length <= 1 && !prop.key && !prop.value}
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            ))}
          </div>
        </div>

        <div className="mb-6">
          <h3 className="text-md font-medium text-gray-700 mb-2">Message Content</h3>
          <div className="bg-gray-50 p-4 rounded-md border border-gray-200 mb-2">
            <TextEditor
              value={messageText}
              onChange={setMessageText}
              language={contentType.includes('json') ? 'json' : contentType.includes('xml') ? 'xml' : 'plaintext'}
              height="200px"
              placeholder={getFormattedExample()}
            />
          </div>
          <div className="flex justify-end">
            {messageText.trim() === '' && (
              <button
                type="button"
                onClick={() => setMessageText(getFormattedExample())}
                className="text-primary-600 hover:text-primary-800 text-sm"
              >
                Insert example message
              </button>
            )}
          </div>
        </div>

        <div className="flex justify-end">
          <button
            type="submit"
            disabled={isPublishing || !selectedDestination || !messageText.trim()}
            className={`bg-primary-600 text-white px-4 py-2 rounded-md hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 ${
              isPublishing || !selectedDestination || !messageText.trim() ? 'opacity-50 cursor-not-allowed' : ''
            }`}
          >
            {isPublishing ? 'Publishing...' : 'Publish Message'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default TibcoPublisher; 