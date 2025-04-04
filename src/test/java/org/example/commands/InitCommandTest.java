package org.example.commands;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.example.interfaces.ICommand;
import org.example.interfaces.IDependency;
import org.example.interfaces.IMovable;
import org.example.ioc.IoC;
import org.example.ioc.IocContextCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.*;

class InitCommandTest {

    @BeforeEach
    void init() {
        IocContextCleaner.clean();
        InitCommand initCommand = new InitCommand();
        InitCommand.initialized = false;
        initCommand.execute();
    }

    @Test
    void shouldReturnCurrentScope() {
        ConcurrentMap<String, IDependency> resolve = IoC.resolve("IoC.Scope.Current", new Object[]{});
        assertNotNull(resolve);
        assertEquals(10, resolve.size());
        assertTrue(resolve.containsKey("IoC.Scope.Create"));
        assertTrue(resolve.containsKey("IoC.Scope.Current"));
        assertTrue(resolve.containsKey("IoC.Scope.Current.Clear"));
        assertTrue(resolve.containsKey("IoC.Scope.Parent"));
        assertTrue(resolve.containsKey("IoC.Scope.Current.Set"));
        assertTrue(resolve.containsKey("IoC.Scope.Create.Empty"));
        assertTrue(resolve.containsKey("IoC.Register"));
    }

    @Test
    void shouldChangeScopeToNewOne() {
        ConcurrentMap<String, IDependency> parentScope = IoC.resolve("IoC.Scope.Current", new Object[]{});

        ConcurrentMap<String, IDependency> createdScope = IoC.resolve("IoC.Scope.Create", new Object[]{});
        assertNotNull(createdScope);

        ICommand setScopeCommand = IoC.resolve("IoC.Scope.Current.Set", new Object[]{createdScope});
        setScopeCommand.execute();

        ConcurrentMap<String, IDependency> childScope = IoC.resolve("IoC.Scope.Current", new Object[]{});

        assertNotNull(parentScope);
        assertEquals(childScope, createdScope);
        assertNotEquals(childScope, parentScope);
    }

    @Test
    void shouldGenerateAnapterByInterface() {
        IMovable movableAdapter = IoC.resolve("Adapter", new Object[]{ IMovable.class, new HashMap<>() });

        resolveGetPositionBean();
        resolveSetPositionBean();
        resolveGetVelocityBean();

        Vector desiredPosition = new Vector();
        assertDoesNotThrow(() -> movableAdapter.setPosition(desiredPosition));

        Vector actualPosition = assertDoesNotThrow(() -> movableAdapter.getPosition());
        assertEquals(desiredPosition, actualPosition);

        assertDoesNotThrow(() -> movableAdapter.getVelocity());

        ConcurrentMap<String, IDependency> currentScope = IoC.resolve("IoC.Scope.Current", new Object[]{});
        assertCurrentScopeHoldsAdapter(currentScope);
        movableAdapter.finish();
        assertCurrentScopeDoesntHoldAdapter(currentScope);
    }

    @Test
    void shouldCreateScopeWithDesiredParentScope() {
        ConcurrentMap<String, IDependency> desiredParentScope = IoC.resolve("IoC.Scope.Create", new Object[]{});

        ConcurrentMap<String, IDependency> createdScope = IoC.resolve("IoC.Scope.Create", new Object[]{desiredParentScope});
        assertNotNull(createdScope);
        ICommand setScopeCommand = IoC.resolve("IoC.Scope.Current.Set", new Object[]{createdScope});
        setScopeCommand.execute();

        ConcurrentMap<String, IDependency> parentScope = IoC.resolve("IoC.Scope.Parent", new Object[]{createdScope});

        assertEquals(desiredParentScope, parentScope);
    }

    @Test
    void shouldNotClearRootScope() {
        ConcurrentMap<String, IDependency> currentScope = IoC.resolve("IoC.Scope.Current", new Object[]{});
        assertEquals(10, currentScope.size());

        ICommand clearScopeCommand = IoC.resolve("IoC.Scope.Current.Clear", new Object[]{currentScope});
        clearScopeCommand.execute();
        ConcurrentMap<String, IDependency> updatedCurrentScope = IoC.resolve("IoC.Scope.Current", new Object[]{});

        assertFalse(updatedCurrentScope.isEmpty());
    }

    @Test
    void shouldSwitchFromLocalScopeToRootScopeWhenScopeWasClear() {
        ConcurrentMap<String, IDependency> createdScope = IoC.resolve("IoC.Scope.Create", new Object[]{});
        assertNotNull(createdScope);

        ICommand setScopeCommand = IoC.resolve("IoC.Scope.Current.Set", new Object[]{createdScope});
        setScopeCommand.execute();
        ConcurrentMap<String, IDependency> currentScope = IoC.resolve("IoC.Scope.Current", new Object[]{});
        assertEquals(1, currentScope.size());
        Object registerDependencyCommand = IoC.resolve("IoC.Register", new Object[]{"IoC.newDependency", new IDependency() {
            @Override
            public Object invoke(Object[] args) {
                return new ICommand() {
                    @Override
                    public void execute() {

                    }
                };
            }
        }});
        ((ICommand) registerDependencyCommand).execute();
        assertEquals(2, currentScope.size());

        ICommand clearScopeCommand = IoC.resolve("IoC.Scope.Current.Clear", new Object[]{currentScope});
        clearScopeCommand.execute();
        assertThrows(RuntimeException.class, () -> IoC.resolve("IoC.Scope.Parent", new Object[]{}));
    }

    @Test
    void shouldRegisterNewStrategy() {
        MutableBoolean isNewDependencyInvoked = new MutableBoolean();
        Object registerDependencyCommand = IoC.resolve("IoC.Register", new Object[]{"IoC.newDependency", new IDependency() {
            @Override
            public Object invoke(Object[] args) {
                return new ICommand() {
                    @Override
                    public void execute() {
                        ((MutableBoolean) isNewDependencyInvoked).setValue(Boolean.TRUE);
                    }
                };
            }
        }});
        ((ICommand) registerDependencyCommand).execute();

        ICommand newDependency = IoC.resolve("IoC.newDependency", new Object[]{isNewDependencyInvoked});

        assertFalse(isNewDependencyInvoked.getValue());
        newDependency.execute();
        assertTrue(isNewDependencyInvoked.getValue());
    }


    @Test
    void shouldUseDifferentScopesForDifferentThreads() {
        MutableBoolean wasScopeCreated = new MutableBoolean();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            synchronized (wasScopeCreated) {
                createAndSetNewScope();
                ICommand parentScope = IoC.resolve("IoC.Scope.Parent", new Object[]{});
                assertDoesNotThrow(parentScope::execute);
                wasScopeCreated.setTrue();
            }
        });

        executor.submit(() -> {
            synchronized (wasScopeCreated) {
                ICommand parentScope = IoC.resolve("IoC.Scope.Parent", new Object[]{});
                assertThrows(RuntimeException.class, parentScope::execute);
            }
        });
    }

    private static void createAndSetNewScope() {
        ConcurrentMap<String, IDependency> createdScope = IoC.resolve("IoC.Scope.Create", new Object[]{});
        assertNotNull(createdScope);

        ICommand setScopeCommand = IoC.resolve("IoC.Scope.Current.Set", new Object[]{createdScope});
        setScopeCommand.execute();
    }


    private static void assertCurrentScopeDoesntHoldAdapter(ConcurrentMap<String, IDependency> currentScope) {
        assertNull(currentScope.get("MovableAdapter"));
    }

    private static void assertCurrentScopeHoldsAdapter(ConcurrentMap<String, IDependency> currentScope) {
        assertNotNull(currentScope.get("MovableAdapter"));
    }

    private static void resolveSetPositionBean() {
        ((ICommand) IoC.resolve("IoC.Register", new Object[]{"org.example.interfaces.IMovable:position.set", new IDependency() {
            @Override
            public Object invoke(Object[] args) {
                return new ICommand() {
                    @Override
                    public void execute() {
                        Object uObject = args[0];
                        ((Map) uObject).put("position", args[1]);
                    }
                };
            }
        }})).execute();
    }

    private static void resolveGetVelocityBean() {
        ((ICommand) IoC.resolve("IoC.Register", new Object[]{"org.example.interfaces.IMovable:velocity.get", new IDependency() {
            @Override
            public Object invoke(Object[] args) {
                Object uObject = args[0];
                return ((Map) uObject).get("velocity");
            }
        }})).execute();
    }

    private static void resolveGetPositionBean() {
        ((ICommand) IoC.resolve("IoC.Register", new Object[]{"org.example.interfaces.IMovable:position.get", new IDependency() {
            @Override
            public Object invoke(Object[] args) {
                Object uObject = args[0];
                return ((Map) uObject).get("position");
            }
        }})).execute();
    }

}