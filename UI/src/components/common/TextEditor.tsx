import React from 'react';

interface TextEditorProps {
  value: string;
  onChange: (value: string) => void;
  language?: string; // Kept for compatibility but not used
  height?: string;
  placeholder?: string;
  readOnly?: boolean;
}

const TextEditor: React.FC<TextEditorProps> = ({
  value,
  onChange,
  height = '300px',
  placeholder = '',
  readOnly = false
}) => {
  return (
    <textarea
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      readOnly={readOnly}
      className="w-full font-mono text-sm border border-gray-300 rounded-md focus:ring-primary-500 focus:border-primary-500"
      style={{ 
        height, 
        resize: 'vertical',
        whiteSpace: 'pre',
        overflowWrap: 'normal',
        overflowX: 'auto'
      }}
    />
  );
};

export default TextEditor; 