// Hardcoded protocol data
const protocols = [
  {
    id: 'rest',
    name: 'REST',
    description: 'HTTP/HTTPS protocol for RESTful services',
    isEnabled: true,
    recordingCount: 5,
    stubCount: 12
  },
  {
    id: 'soap',
    name: 'SOAP',
    description: 'XML-based messaging protocol',
    isEnabled: true,
    recordingCount: 3,
    stubCount: 8
  },
  {
    id: 'tibco',
    name: 'TIBCO',
    description: 'TIBCO Enterprise Message Service',
    isEnabled: true,
    recordingCount: 0,
    stubCount: 0
  },
  {
    id: 'ibm-mq',
    name: 'IBM MQ',
    description: 'IBM Message Queue middleware',
    isEnabled: true,
    recordingCount: 1,
    stubCount: 6
  },
  {
    id: 'kafka',
    name: 'Kafka',
    description: 'Event streaming platform',
    isEnabled: true,
    recordingCount: 4,
    stubCount: 10
  },
  {
    id: 'file',
    name: 'File Service',
    description: 'File-based virtual services for file upload/download simulation',
    isEnabled: true,
    recordingCount: 0,
    stubCount: 0
  },
  {
    id: 'activemq',
    name: 'ActiveMQ',
    description: 'Apache ActiveMQ messaging middleware',
    isEnabled: true,
    recordingCount: 0,
    stubCount: 0
  }
];

{protocol.id === 'rest' ? (
  <Link 
    to="/rest" 
    className="inline-block bg-primary-600 text-white py-2 px-4 rounded hover:bg-primary-700 transition-colors w-full text-center"
  >
    Manage REST API
  </Link>
) : protocol.id === 'soap' ? (
  <Link 
    to="/soap" 
    className="inline-block bg-primary-600 text-white py-2 px-4 rounded hover:bg-primary-700 transition-colors w-full text-center"
  >
    Manage SOAP API
  </Link>
) : protocol.id === 'tibco' ? (
  <Link 
    to="/tibco" 
    className="inline-block bg-primary-600 text-white py-2 px-4 rounded hover:bg-primary-700 transition-colors w-full text-center"
  >
    Manage TIBCO EMS
  </Link>
) : protocol.id === 'kafka' ? (
  <Link 
    to="/kafka" 
    className="inline-block bg-primary-600 text-white py-2 px-4 rounded hover:bg-primary-700 transition-colors w-full text-center"
  >
    Manage Kafka
  </Link>
) : protocol.id === 'file' ? (
  <Link 
    to="/file" 
    className="inline-block bg-primary-600 text-white py-2 px-4 rounded hover:bg-primary-700 transition-colors w-full text-center"
  >
    Manage File Service
  </Link>
) : protocol.id === 'activemq' ? (
  <Link 
    to="/activemq" 
    className="inline-block bg-primary-600 text-white py-2 px-4 rounded hover:bg-primary-700 transition-colors w-full text-center"
  >
    Manage ActiveMQ
  </Link>
) : protocol.id === 'ibm-mq' ? (
  <Link 
    to="/ibmmq" 
    className="inline-block bg-primary-600 text-white py-2 px-4 rounded hover:bg-primary-700 transition-colors w-full text-center"
  >
    Manage IBM MQ
  </Link>
) : (
  <div className="inline-block bg-gray-200 text-gray-700 py-2 px-4 rounded w-full text-center">
    Coming Soon
  </div>
)} 