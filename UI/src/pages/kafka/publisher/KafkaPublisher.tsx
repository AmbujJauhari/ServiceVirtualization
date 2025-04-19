import React, { useState, useEffect } from 'react';
import { useGetKafkaTopicsQuery, usePublishKafkaMessageMutation } from '../../../api/kafkaApi';
import TextEditor from '../../../components/common/TextEditor';

interface MessageHeader {
  key: string;
  value: string;
}

const contentTypeOptions = [
  { value: 'application/json', label: 'JSON' },
  { value: 'application/xml', label: 'XML' },
  { value: 'text/plain', label: 'Plain Text' },
  { value: 'avro/binary', label: 'Avro' },
];

const KafkaPublisher: React.FC = () => {
  const [selectedTopic, setSelectedTopic] = useState('');
  const [messageKey, setMessageKey] = useState('');
  const [messageText, setMessageText] = useState('');
  const [contentType, setContentType] = useState(contentTypeOptions[0].value);
  const [headers, setHeaders] = useState<MessageHeader[]>([{ key: '', value: '' }]);
  const [isSuccessful, setIsSuccessful] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const { data: topics, isLoading: isLoadingTopics } = useGetKafkaTopicsQuery();
  const [publishMessage, { isLoading: isPublishing }] = usePublishKafkaMessageMutation();

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

  const addHeader = () => {
    setHeaders([...headers, { key: '', value: '' }]);
  };

  const removeHeader = (index: number) => {
    const updatedHeaders = [...headers];
    updatedHeaders.splice(index, 1);
    setHeaders(updatedHeaders.length ? updatedHeaders : [{ key: '', value: '' }]);
  };

  const handleHeaderChange = (index: number, field: 'key' | 'value', value: string) => {
    const updatedHeaders = [...headers];
    updatedHeaders[index][field] = value;
    setHeaders(updatedHeaders);
  };

  const getFormattedExample = () => {
    switch (contentType) {
      case 'application/json':
        return `{\n  "message": "Hello Kafka",\n  "timestamp": "${new Date().toISOString()}"\n}`;
      case 'application/xml':
        return `<message>\n  <content>Hello Kafka</content>\n  <timestamp>${new Date().toISOString()}</timestamp>\n</message>`;
      case 'avro/binary':
        return `// Avro data is binary and needs to be encoded properly.\n// This is just a placeholder example.`;
      default:
        return 'Hello Kafka!';
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');
    setIsSuccessful(false);
    
    if (!selectedTopic || !messageText.trim()) {
      setErrorMessage('Please select a topic and enter a message.');
      return;
    }

    // Format headers, filtering out empty ones
    const headerMap: Record<string, string> = {};
    headers.forEach(header => {
      if (header.key.trim()) {
        headerMap[header.key.trim()] = header.value;
      }
    });

    try {
      await publishMessage({
        topic: selectedTopic,
        key: messageKey || undefined,
        contentType,
        message: messageText,
        headers: Object.keys(headerMap).length > 0 ? headerMap : undefined
      }).unwrap();
      
      setIsSuccessful(true);
      setSuccessMessage(`Message successfully published to topic "${selectedTopic}"`);
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
          <h3 className="text-md font-medium text-gray-700 mb-2">Topic</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label htmlFor="topic" className="block text-sm font-medium text-gray-700 mb-1">
                Topic Name
              </label>
              <select
                id="topic"
                value={selectedTopic}
                onChange={(e) => setSelectedTopic(e.target.value)}
                className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                required
              >
                <option value="">Select a topic</option>
                {isLoadingTopics ? (
                  <option disabled>Loading topics...</option>
                ) : !topics || topics.length === 0 ? (
                  <option disabled>No topics available</option>
                ) : (
                  topics.map((topic) => (
                    <option key={topic} value={topic}>
                      {topic}
                    </option>
                  ))
                )}
              </select>
            </div>
            
            <div>
              <label htmlFor="messageKey" className="block text-sm font-medium text-gray-700 mb-1">
                Message Key (Optional)
              </label>
              <input
                type="text"
                id="messageKey"
                value={messageKey}
                onChange={(e) => setMessageKey(e.target.value)}
                placeholder="Enter message key"
                className="w-full rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              />
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
              className="w-full md:w-1/3 rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            >
              {contentTypeOptions.map(option => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>
          
          <div className="mb-4">
            <div className="flex justify-between items-center mb-2">
              <label className="block text-sm font-medium text-gray-700">
                Headers
              </label>
              <button
                type="button"
                onClick={addHeader}
                className="text-blue-600 hover:text-blue-800 text-sm font-medium flex items-center"
              >
                <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                </svg>
                Add Header
              </button>
            </div>
            
            <div className="space-y-2">
              {headers.map((header, index) => (
                <div key={index} className="flex space-x-2">
                  <input
                    type="text"
                    placeholder="Header name"
                    value={header.key}
                    onChange={(e) => handleHeaderChange(index, 'key', e.target.value)}
                    className="w-1/3 rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  />
                  <input
                    type="text"
                    placeholder="Value"
                    value={header.value}
                    onChange={(e) => handleHeaderChange(index, 'value', e.target.value)}
                    className="w-1/2 rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  />
                  <button
                    type="button"
                    onClick={() => removeHeader(index)}
                    className="px-3 py-2 bg-red-100 text-red-600 rounded hover:bg-red-200"
                  >
                    ✕
                  </button>
                </div>
              ))}
            </div>
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
                className="text-blue-600 hover:text-blue-800 text-sm"
              >
                Insert example message
              </button>
            )}
          </div>
        </div>

        <div className="flex justify-end">
          <button
            type="submit"
            disabled={isPublishing || !selectedTopic || !messageText.trim()}
            className={`bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 ${
              isPublishing || !selectedTopic || !messageText.trim() ? 'opacity-50 cursor-not-allowed' : ''
            }`}
          >
            {isPublishing ? 'Publishing...' : 'Publish Message'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default KafkaPublisher; 