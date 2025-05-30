import asyncio
import websockets
import json
import logging # For better logging output
import sys   # To check if running in Jupyter/IPython

# Configure logging to display INFO messages and above
logging.basicConfig(level=logging.INFO, stream=sys.stdout, format='%(levelname)s:%(message)s')

async def handle_connection(websocket):
    """
    Handles a new WebSocket connection, receives messages, and prints them.
    """
    client_ip = websocket.remote_address[0]
    logging.info(f"Client connected from {client_ip}")

    try:
        async for message in websocket:
            try:
                # Attempt to parse the message as JSON
                data = json.loads(message)

                # Print parsed data in a readable format
                timestamp_ms = data.get("timestamp")
                if timestamp_ms:
                    # Convert ms timestamp to seconds for better readability
                    timestamp_s = timestamp_ms / 1000.0
                    logging.info(f"--- Timestamp: {timestamp_s:.3f} s ---")
                else:
                    logging.info("--- Data Received ---")

                if "accelerometer" in data:
                    accel = data["accelerometer"]
                    logging.info(f"  Accelerometer: X={accel.get('x'):.2f}, Y={accel.get('y'):.2f}, Z={accel.get('z'):.2f}")

                if "gyroscope" in data:
                    gyro = data["gyroscope"]
                    logging.info(f"  Gyroscope: Gx={gyro.get('gx'):.2f}, Gy={gyro.get('gy'):.2f}, Gz={gyro.get('gz'):.2f}")

            except json.JSONDecodeError:
                logging.warning(f"Received non-JSON message: {message}")
            except Exception as e:
                logging.error(f"Error processing message: {e}", exc_info=True)

    except websockets.exceptions.ConnectionClosedOK:
        logging.info("Client disconnected gracefully.")
    except websockets.exceptions.ConnectionClosedError as e:
        logging.error(f"Client disconnected with error: {e}")
    except Exception as e:
        logging.error(f"Unexpected error in connection handler: {e}", exc_info=True)
    finally:
        logging.info(f"Connection handler finished for {client_ip}")

async def main():
    """
    Starts the WebSocket server.
    """
    # Host on "0.0.0.0" to accept connections from any IP address on your network.
    # For Android Emulator: The Android app will connect to ws://10.0.2.2:8080
    # For Physical Android Device: You MUST use your computer's actual local IP address (e.g., "ws://192.168.1.100")
    # You can find your computer's IP by running 'ipconfig' (Windows) or 'ifconfig' (macOS/Linux) in your terminal.

    host = "0.0.0.0"
    port = 8080

    logging.info(f"Starting WebSocket server on ws://{host}:{port}")

    # This loop will run forever, listening for connections
    async with websockets.serve(handle_connection, host, port):
        # We need to keep the server running. In a notebook, this await will keep the cell "busy".
        # However, for a background server, we use create_task below.
        # If running as a standalone script, this effectively keeps it alive.
        await asyncio.Future()

# This block ensures the code runs correctly both as a standalone script
# and within an interactive environment like Jupyter.
if __name__ == "__main__":
    try:
        # Check if an event loop is already running (common in Jupyter/IPython)
        try:
            loop = asyncio.get_running_loop()
        except RuntimeError: # No running loop
            loop = None

        if loop and loop.is_running():
            # If a loop is running, just schedule the main coroutine as a task.
            # This allows the Jupyter cell to "complete" while the server runs in the background.
            loop.create_task(main())
            logging.info("WebSocket server scheduled to run in background. To stop, you might need to interrupt the kernel.")
        else:
            # If no loop is running (e.g., standard script execution), run it normally.
            logging.info("Running WebSocket server directly.")
            asyncio.run(main())

    except KeyboardInterrupt:
        logging.info("Server stopped by user (Ctrl+C).")
    except Exception as e:
        logging.critical(f"Server crashed: {e}", exc_info=True)