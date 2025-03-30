package org.example.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.example.interfaces.ICommand;
import org.example.interfaces.IDependency;
import org.example.interfaces.IMovable;
import org.example.ioc.IoC;
import org.example.ioc.IocContextCleaner;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        assertEquals(9, resolve.size());
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
    void shouldObtainAdapterRetrievedFromIoCAndGenerateAdapter() throws IOException {
        IMovable movableAdapter = IoC.resolve("Adapter", new Object[]{IMovable.class, new HashMap<>()});
        String expectedMovableAdapterFilename = "ExpectedMovableAdapter.java";

        StringBuilder expectedMovableAdapter = new StringBuilder();
        try (InputStream is = InitCommandTest.class.getClassLoader().getResourceAsStream(expectedMovableAdapterFilename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    expectedMovableAdapter.append(line);
                }
            }

        // с get у нас проблемы: они должны что-то возвращать. но через IoC.Register мы задаем команду, которая ничего не вернет. или необязательно? Можно вернуть что-то еще?
        // да, например, так сделан IoC.Scope.Current, вот как мы его дергаем:
        /// ConcurrentMap<String, IDependency> parentScope = IoC.resolve("IoC.Scope.Current", new Object[]{});

        // org.example.interfaces.IMovable:position.get
        ((ICommand) IoC.resolve("IoC.Register", new Object[]{"org.example.interfaces.IMovable:position.get", new IDependency() {
            @Override
            public Object invoke(Object[] args) {
                return new Vector<>();
            }
        }})).execute();

        // org.example.interfaces.IMovable:velocity.get
        ((ICommand) IoC.resolve("IoC.Register", new Object[]{"org.example.interfaces.IMovable:velocity.get", new IDependency() {
            @Override
            public Object invoke(Object[] args) {
                return new Vector<>();
            }
        }})).execute();


        // org.example.interfaces.IMovable:position.set
        ((ICommand) IoC.resolve("IoC.Register", new Object[]{"org.example.interfaces.IMovable:position.set", new IDependency() {
            @Override
            public Object invoke(Object[] args) {
                return new ICommand() {
                    @Override
                    public void execute() {
                    }
                };
            }
        }})).execute();

        System.out.println(expectedMovableAdapter);

        assertDoesNotThrow(() -> movableAdapter.setPosition(new Vector()));
        assertDoesNotThrow(() -> movableAdapter.getPosition());
        assertDoesNotThrow(() -> movableAdapter.getVelocity());

        // мне тут осталось ошибку пофиксить, которая связана с первым запуском
        // также подумать над пунктом 3:
        /*
        * interface Spaceship.Operations.IMovable
            {
                Vector getPosition();
                Vector setPosition(Vector newValue);
                Vector getVelocity();

                void finish(); // для подобных методов нужна реализация
            }
            * а ну так там тоже будет IoC-зависимость, но как мы оттуда получим доступ к полям класса? как мы собираемся
            * получать доступ к полям класса в getPosition и проч? хуйня какая-то
            *
            * а ну для полей: мы знаем филдовое название атрибута, в getPosition, например, мы в иок сам объект передаем,
            * в новой зависимости org.example.interfaces.IMovable:position.get мы укажем, что именно из этого объекта
            * забирать
            *
            * в для finish, например, можно все ассоциированные с адаптером объекты уничтожить, правильно?
        * */
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
        assertEquals(9, currentScope.size());

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

}