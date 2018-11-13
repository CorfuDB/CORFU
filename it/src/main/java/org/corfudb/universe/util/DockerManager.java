package org.corfudb.universe.util;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.ExecCreation;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.corfudb.universe.node.NodeException;

import java.time.Duration;

@Builder
@Slf4j
public class DockerManager {

    @NonNull
    private final DockerClient docker;

    public void stop(String containerName, Duration timeout) {
        log.info("Stopping the Corfu server. Docker container: {}", containerName);

        try {
            ContainerInfo container = docker.inspectContainer(containerName);
            if (!container.state().running() && !container.state().paused()) {
                log.warn("The container `{}` is already stopped", container.name());
                return;
            }
            docker.stopContainer(containerName, (int) timeout.getSeconds());
        } catch (DockerException | InterruptedException e) {
            throw new NodeException("Can't stop Corfu server: " + containerName, e);
        }
    }

    public void kill(String containerName) {
        log.info("Killing docker container: {}", containerName);

        try {
            ContainerInfo container = docker.inspectContainer(containerName);

            if (!container.state().running() && !container.state().paused()) {
                log.warn("The container `{}` is not running", container.name());
                return;
            }
            docker.killContainer(containerName);
        } catch (DockerException | InterruptedException ex) {
            throw new NodeException("Can't kill Corfu server: " + containerName, ex);
        }
    }

    public void destroy(String containerName) {
        log.info("Destroying docker container: {}", containerName);

        try {
            kill(containerName);
        } catch (NodeException ex) {
            log.warn("Can't kill container: {}", containerName);
        }

        try {
            docker.removeContainer(containerName);
        } catch (DockerException | InterruptedException ex) {
            throw new NodeException("Can't destroy Corfu server. Already deleted. Container: " + containerName, ex);
        }
    }

    public void pause(String containerName) {
        log.info("Pausing container: {}", containerName);

        try {
            ContainerInfo container = docker.inspectContainer(containerName);
            if (!container.state().running()) {
                log.warn("The container `{}` is not running", container.name());
                return;
            }
            docker.pauseContainer(containerName);
        } catch (DockerException | InterruptedException ex) {
            throw new NodeException("Can't pause container " + containerName, ex);
        }
    }

    public void start(String containerName) {
        log.info("Starting docker container: {}", containerName);

        try {
            ContainerInfo container = docker.inspectContainer(containerName);
            if (container.state().running() || container.state().paused()) {
                log.warn("The container `{}` already running, should stop before start", container.name());
                return;
            }
            docker.startContainer(containerName);
        } catch (DockerException | InterruptedException ex) {
            throw new NodeException("Can't start container " + containerName, ex);
        }
    }

    public void restart(String containerName) {
        log.info("Restarting the corfu server: {}", containerName);

        try {
            ContainerInfo container = docker.inspectContainer(containerName);
            if (container.state().running() || container.state().paused()) {
                log.warn("The container `{}` already running, should stop before restart", container.name());
                return;
            }
            docker.restartContainer(containerName);
        } catch (DockerException | InterruptedException ex) {
            throw new NodeException("Can't restart container " + containerName, ex);
        }
    }

    public void resume(String containerName) {
        log.info("Resuming docker container: {}", containerName);

        try {
            ContainerInfo container = docker.inspectContainer(containerName);
            if (!container.state().paused()) {
                log.warn("The container `{}` is not paused, should pause before resuming", container.name());
                return;
            }
            docker.unpauseContainer(containerName);
        } catch (DockerException | InterruptedException ex) {
            throw new NodeException("Can't resume container " + containerName, ex);
        }
    }

    /**
     * Run `docker exec` on a container
     */
    public void execCommand(String containerName,  String... command) throws DockerException, InterruptedException {
        log.info("Executing docker command: {}", String.join(" ", command));

        ExecCreation execCreation = docker.execCreate(
                containerName,
                command,
                DockerClient.ExecCreateParam.attachStdout(),
                DockerClient.ExecCreateParam.attachStderr()
        );

        docker.execStart(execCreation.id());
    }
}
