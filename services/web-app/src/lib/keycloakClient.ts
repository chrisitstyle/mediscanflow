import Keycloak from "keycloak-js";

let keycloakClient: Keycloak | null = null;
let keycloakInitPromise: Promise<boolean> | null = null;

function getRequiredEnvValue(value: string | undefined, name: string) {
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`);
  }

  return value;
}

export function getKeycloakClient() {
  if (!keycloakClient) {
    keycloakClient = new Keycloak({
      url: getRequiredEnvValue(
        process.env.NEXT_PUBLIC_KEYCLOAK_URL,
        "NEXT_PUBLIC_KEYCLOAK_URL",
      ),
      realm: getRequiredEnvValue(
        process.env.NEXT_PUBLIC_KEYCLOAK_REALM,
        "NEXT_PUBLIC_KEYCLOAK_REALM",
      ),
      clientId: getRequiredEnvValue(
        process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID,
        "NEXT_PUBLIC_KEYCLOAK_CLIENT_ID",
      ),
    });
  }

  return keycloakClient;
}

export function initKeycloak() {
  if (keycloakInitPromise) {
    return keycloakInitPromise;
  }

  const keycloak = getKeycloakClient();

  keycloakInitPromise = keycloak.init({
    onLoad: "check-sso",
    pkceMethod: "S256",
    checkLoginIframe: false,
  });

  return keycloakInitPromise;
}
