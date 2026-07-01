export type SystemComponentStatus = {
  status: "UP" | "DOWN" | "UNKNOWN" | string;
};

export type SystemStatusResponse = {
  status: "UP" | "DOWN" | "UNKNOWN" | string;
  components: {
    database?: SystemComponentStatus;
    rabbitmq?: SystemComponentStatus;
    minio?: SystemComponentStatus;
    aiWorker?: SystemComponentStatus;
    [key: string]: SystemComponentStatus | undefined;
  };
};
