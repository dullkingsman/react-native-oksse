# react-native-oksse

A wrapper around the [oksse](https://github.com/heremaps/oksse) android package for support in react-native.

## Installation

```sh
yarn add react-native-oksse
```

## Usage

```typescript
import EventSource, { RetryError, SSEMessage } from 'react-native-oksse';

// ...

const url = '<some_url>';

// ...

const eventSource = new EventSource(url);

const customEvent = "CUSTOM_EVENT";

const onmessageHandler = (message: SSEMessage) => console.log({ message });

const customEventHandler = (message: SSEMessage) => console.log({ [customEvent]: { message } });

// ...

// add listener for source event
eventSource.onmessage = onmessageHandler;

// ...

// add listener for events comming form the server
eventSource.addEventListener(customEvent, customEventHandler);

// ...

// remove listeners
eventSource.remove(onmessageHandler);
eventSource.remove(customEventHandler);

// close the connection and remove the event source
eventSource.close()
```

## Configuration

`EventSource` takes an optional configuration object other than the url.

```typescript
new EventSource(url: string, config?: SSEConnConfig);
```

### Options

```typescript
interface SSEConnConfig {
  headers?: { [key: string]: string },
  query?: { [key: string]: string }
};
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---
