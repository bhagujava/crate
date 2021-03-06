.. _ref-drop-user:

=============
``DROP USER``
=============

Drop an existing database user.

.. rubric:: Table of Contents

.. contents::
   :local:

Synopsis
========

.. code-block:: psql

  DROP USER [ IF EXISTS ] username;

Description
===========

``DROP USER`` is a management statement to remove an existing database user
from the CrateDB cluster.

For usage of the ``DROP USER`` statement see
:ref:`administration_user_management`.

Parameters
==========

:IF EXISTS:
  Do not fail if the user doesn't exist.

:username:
  The unique name of the database user to be removed.

  The name follows the principles of a SQL identifier (see
  :ref:`sql_lexical_keywords_identifiers`).
