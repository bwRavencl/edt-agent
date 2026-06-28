/*
 * Copyright (C) 2026 Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.bwravencl.edtagent;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.MethodModel;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/// Java agent that injects EDT-violation checks into `javax.swing.J*` accessor
/// methods.
///
/// Bytecode rewriting is done with the [ClassFile] API.
///
/// @see Instrumentation
/// @see ClassFileTransformer
public final class EdtAgent {

	/// Registers the [ClassFileTransformer] with the JVM, as required by the
	/// `-javaagent` mechanism.
	///
	/// @param args unused
	/// @param inst supplied by the JVM
	public static void premain(final String args, final Instrumentation inst) {
		inst.addTransformer(new Transformer());
	}

	/// Inserts an EDT check at the start of an accessor method's body.
	private static final class EdtCheckInjector implements CodeTransform {

		/// Descriptor of the [java.awt.EventQueue] class.
		private static final ClassDesc CD_EVENT_QUEUE = ClassDesc.of("java.awt.EventQueue");

		/// Descriptor of the [Thread] class.
		private static final ClassDesc CD_THREAD = ClassDesc.of("java.lang.Thread");

		/// `()V`, the descriptor of [Thread#dumpStack()].
		private static final MethodTypeDesc MTD_DUMP_STACK = MethodTypeDesc.of(ClassDesc.ofDescriptor("V"));

		/// `()Z`, the descriptor of [java.awt.EventQueue#isDispatchThread].
		private static final MethodTypeDesc MTD_IS_DISPATCH_THREAD = MethodTypeDesc.of(ClassDesc.ofDescriptor("Z"));

		@Override
		public void accept(final CodeBuilder codeBuilder, final CodeElement codeElement) {
			codeBuilder.with(codeElement);
		}

		@Override
		public void atStart(final CodeBuilder codeBuilder) {
			final var afterCheckLabel = codeBuilder.newLabel();
			codeBuilder.invokestatic(CD_EVENT_QUEUE, "isDispatchThread", MTD_IS_DISPATCH_THREAD).ifne(afterCheckLabel)
					.invokestatic(CD_THREAD, "dumpStack", MTD_DUMP_STACK).labelBinding(afterCheckLabel);
		}
	}

	/// Rewrites `javax.swing.J*` classes as they are loaded, injecting EDT checks
	/// into their accessor methods.
	private static final class Transformer implements ClassFileTransformer {

		/// Shared [ClassFile] instance used for parsing and transforming.
		private static final ClassFile CLASS_FILE = ClassFile.of();

		/// Returns `true` if `methodModel`'s name starts with `get`, `is`, or `set`
		///
		/// @param methodModel the method to check
		/// @return whether the method is a getter, is accessor, or setter
		private static boolean isAccessorMethod(final MethodModel methodModel) {
			final var name = methodModel.methodName().stringValue();
			return name.startsWith("get") || name.startsWith("is") || name.startsWith("set");
		}

		@Override
		public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined,
				final ProtectionDomain protectionDomain, final byte[] classFileBuffer) {

			// Process all classes in the `javax.swing` package whose names start with 'J'.
			if (className.startsWith("javax/swing/J")) {
				final var classModel = CLASS_FILE.parse(classFileBuffer);

				final var classTransform = ClassTransform.transformingMethodBodies(Transformer::isAccessorMethod,
						CodeTransform.ofStateful(EdtCheckInjector::new));

				return CLASS_FILE.transformClass(classModel, classTransform);
			}
			return classFileBuffer;
		}
	}
}
